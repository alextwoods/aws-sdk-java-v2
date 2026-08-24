package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

import java.time.Instant;
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
 * One data point related to one contributor.
 *
 * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetInsightRuleReport.html">GetInsightRuleReport</a> and <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_InsightRuleContributor.html">InsightRuleContributor</a>.
 */
@SmithyGenerated
public final class InsightRuleContributorDatapoint implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.INSIGHT_RULE_CONTRIBUTOR_DATAPOINT;
    private static final Schema $SCHEMA_TIMESTAMP = $SCHEMA.member("Timestamp");
    private static final Schema $SCHEMA_APPROXIMATE_VALUE = $SCHEMA.member("ApproximateValue");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient Instant timestamp;
    private final transient Double approximateValue;

    private InsightRuleContributorDatapoint(Builder builder) {
        this.timestamp = builder.timestamp;
        this.approximateValue = builder.approximateValue;
    }

    /**
     * The timestamp of the data point.
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * The approximate value that this contributor added during this timestamp.
     */
    public Double getApproximateValue() {
        return approximateValue;
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
        InsightRuleContributorDatapoint that = (InsightRuleContributorDatapoint) other;
        return Objects.equals(this.approximateValue, that.approximateValue)
               && Objects.equals(this.timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(timestamp);
        $hc = 31 * $hc + Objects.hashCode(approximateValue);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (timestamp != null) {
            serializer.writeTimestamp($SCHEMA_TIMESTAMP, timestamp);
        }
        if (approximateValue != null) {
            serializer.writeDouble($SCHEMA_APPROXIMATE_VALUE, approximateValue);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TIMESTAMP, member, timestamp);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_APPROXIMATE_VALUE, member, approximateValue);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link InsightRuleContributorDatapoint}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.timestamp(this.timestamp);
        builder.approximateValue(this.approximateValue);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link InsightRuleContributorDatapoint}.
     */
    public static final class Builder implements ShapeBuilder<InsightRuleContributorDatapoint> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private Instant timestamp;
        private Double approximateValue;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_TIMESTAMP);
            tracker.setMember($SCHEMA_APPROXIMATE_VALUE);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The timestamp of the data point.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder timestamp(Instant timestamp) {
            this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
            tracker.setMember($SCHEMA_TIMESTAMP);
            return this;
        }

        /**
         * The approximate value that this contributor added during this timestamp.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder approximateValue(Double approximateValue) {
            this.approximateValue = approximateValue;
            tracker.setMember($SCHEMA_APPROXIMATE_VALUE);
            return this;
        }

        @Override
        public InsightRuleContributorDatapoint build() {
            tracker.validate();
            return new InsightRuleContributorDatapoint(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> timestamp((Instant) SchemaUtils.validateSameMember($SCHEMA_TIMESTAMP, member, value));
                case 1 -> approximateValue((Double) SchemaUtils.validateSameMember($SCHEMA_APPROXIMATE_VALUE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<InsightRuleContributorDatapoint> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TIMESTAMP)) {
                timestamp(Instant.EPOCH);
            }
            if (!tracker.checkMember($SCHEMA_APPROXIMATE_VALUE)) {
                tracker.setMember($SCHEMA_APPROXIMATE_VALUE);
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
                    case 0 -> builder.timestamp(de.readTimestamp(member));
                    case 1 -> builder.approximateValue(de.readDouble(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
