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

/**
 * One of the unique contributors found by a Contributor Insights rule. If the rule contains multiple keys, then a
 * unique contributor is a unique combination of values from all the keys in the rule.
 *
 * <p>If the rule contains a single key, then each unique contributor is each unique value for this key.
 *
 * <p>For more information, see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_GetInsightRuleReport.html">GetInsightRuleReport</a>.
 */
@SmithyGenerated
public final class InsightRuleContributor implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.INSIGHT_RULE_CONTRIBUTOR;
    private static final Schema $SCHEMA_KEYS = $SCHEMA.member("Keys");
    private static final Schema $SCHEMA_APPROXIMATE_AGGREGATE_VALUE = $SCHEMA.member("ApproximateAggregateValue");
    private static final Schema $SCHEMA_DATAPOINTS = $SCHEMA.member("Datapoints");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<String> keys;
    private final transient Double approximateAggregateValue;
    private final transient List<InsightRuleContributorDatapoint> datapoints;

    private InsightRuleContributor(Builder builder) {
        this.keys = builder.keys == null ? null : Collections.unmodifiableList(builder.keys);
        this.approximateAggregateValue = builder.approximateAggregateValue;
        this.datapoints = builder.datapoints == null ? null : Collections.unmodifiableList(builder.datapoints);
    }

    /**
     * One of the log entry field keywords that is used to define contributors for this rule.
     */
    public List<String> getKeys() {
        if (keys == null) {
            return Collections.emptyList();
        }
        return keys;
    }

    public boolean hasKeys() {
        return keys != null;
    }

    /**
     * An approximation of the aggregate value that comes from this contributor.
     */
    public Double getApproximateAggregateValue() {
        return approximateAggregateValue;
    }

    /**
     * An array of the data points where this contributor is present. Only the data points when this contributor
     * appeared are included in the array.
     */
    public List<InsightRuleContributorDatapoint> getDatapoints() {
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
        InsightRuleContributor that = (InsightRuleContributor) other;
        return Objects.equals(this.approximateAggregateValue, that.approximateAggregateValue)
               && Objects.equals(this.keys, that.keys)
               && Objects.equals(this.datapoints, that.datapoints);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(keys);
        $hc = 31 * $hc + Objects.hashCode(approximateAggregateValue);
        $hc = 31 * $hc + Objects.hashCode(datapoints);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (keys != null) {
            serializer.writeList($SCHEMA_KEYS, keys, keys.size(), SharedSerde.InsightRuleContributorKeysSerializer.INSTANCE);
        }
        if (approximateAggregateValue != null) {
            serializer.writeDouble($SCHEMA_APPROXIMATE_AGGREGATE_VALUE, approximateAggregateValue);
        }
        if (datapoints != null) {
            serializer.writeList($SCHEMA_DATAPOINTS, datapoints, datapoints.size(), SharedSerde.InsightRuleContributorDatapointsSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_KEYS, member, keys);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_APPROXIMATE_AGGREGATE_VALUE, member, approximateAggregateValue);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_DATAPOINTS, member, datapoints);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link InsightRuleContributor}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.keys(this.keys);
        builder.approximateAggregateValue(this.approximateAggregateValue);
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
     * Builder for {@link InsightRuleContributor}.
     */
    public static final class Builder implements ShapeBuilder<InsightRuleContributor> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private List<String> keys;
        private Double approximateAggregateValue;
        private List<InsightRuleContributorDatapoint> datapoints;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_KEYS);
            tracker.setMember($SCHEMA_APPROXIMATE_AGGREGATE_VALUE);
            tracker.setMember($SCHEMA_DATAPOINTS);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * One of the log entry field keywords that is used to define contributors for this rule.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder keys(List<String> keys) {
            this.keys = Objects.requireNonNull(keys, "keys cannot be null");
            tracker.setMember($SCHEMA_KEYS);
            return this;
        }

        /**
         * An approximation of the aggregate value that comes from this contributor.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder approximateAggregateValue(Double approximateAggregateValue) {
            this.approximateAggregateValue = approximateAggregateValue;
            tracker.setMember($SCHEMA_APPROXIMATE_AGGREGATE_VALUE);
            return this;
        }

        /**
         * An array of the data points where this contributor is present. Only the data points when this contributor
         * appeared are included in the array.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder datapoints(List<InsightRuleContributorDatapoint> datapoints) {
            this.datapoints = Objects.requireNonNull(datapoints, "datapoints cannot be null");
            tracker.setMember($SCHEMA_DATAPOINTS);
            return this;
        }

        @Override
        public InsightRuleContributor build() {
            tracker.validate();
            return new InsightRuleContributor(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> keys((List<String>) SchemaUtils.validateSameMember($SCHEMA_KEYS, member, value));
                case 1 -> approximateAggregateValue((Double) SchemaUtils.validateSameMember($SCHEMA_APPROXIMATE_AGGREGATE_VALUE, member, value));
                case 2 -> datapoints((List<InsightRuleContributorDatapoint>) SchemaUtils.validateSameMember($SCHEMA_DATAPOINTS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<InsightRuleContributor> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_KEYS)) {
                keys(Collections.emptyList());
            }
            if (!tracker.checkMember($SCHEMA_APPROXIMATE_AGGREGATE_VALUE)) {
                tracker.setMember($SCHEMA_APPROXIMATE_AGGREGATE_VALUE);
            }
            if (!tracker.checkMember($SCHEMA_DATAPOINTS)) {
                datapoints(Collections.emptyList());
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
                    case 0 -> builder.keys(SharedSerde.deserializeInsightRuleContributorKeys(member, de));
                    case 1 -> builder.approximateAggregateValue(de.readDouble(member));
                    case 2 -> builder.datapoints(SharedSerde.deserializeInsightRuleContributorDatapoints(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
