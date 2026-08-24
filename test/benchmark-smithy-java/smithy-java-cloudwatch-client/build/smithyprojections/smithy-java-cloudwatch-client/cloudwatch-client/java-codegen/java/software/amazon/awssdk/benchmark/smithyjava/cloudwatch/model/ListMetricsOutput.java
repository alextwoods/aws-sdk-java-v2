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
public final class ListMetricsOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.LIST_METRICS_OUTPUT;
    private static final Schema $SCHEMA_METRICS = $SCHEMA.member("Metrics");
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");
    private static final Schema $SCHEMA_OWNING_ACCOUNTS = $SCHEMA.member("OwningAccounts");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<Metric> metrics;
    private final transient String nextToken;
    private final transient List<String> owningAccounts;

    private ListMetricsOutput(Builder builder) {
        this.metrics = builder.metrics == null ? null : Collections.unmodifiableList(builder.metrics);
        this.nextToken = builder.nextToken;
        this.owningAccounts = builder.owningAccounts == null ? null : Collections.unmodifiableList(builder.owningAccounts);
    }

    /**
     * The metrics that match your request.
     */
    public List<Metric> getMetrics() {
        if (metrics == null) {
            return Collections.emptyList();
        }
        return metrics;
    }

    public boolean hasMetrics() {
        return metrics != null;
    }

    /**
     * The token that marks the start of the next batch of returned results.
     */
    public String getNextToken() {
        return nextToken;
    }

    /**
     * If you are using this operation in a monitoring account, this array contains the account IDs of the source
     * accounts where the metrics in the returned data are from.
     *
     * <p>This field is a 1:1 mapping between each metric that is returned and the ID of the owning account.
     */
    public List<String> getOwningAccounts() {
        if (owningAccounts == null) {
            return Collections.emptyList();
        }
        return owningAccounts;
    }

    public boolean hasOwningAccounts() {
        return owningAccounts != null;
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
        ListMetricsOutput that = (ListMetricsOutput) other;
        return Objects.equals(this.nextToken, that.nextToken)
               && Objects.equals(this.metrics, that.metrics)
               && Objects.equals(this.owningAccounts, that.owningAccounts);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(metrics);
        $hc = 31 * $hc + Objects.hashCode(nextToken);
        $hc = 31 * $hc + Objects.hashCode(owningAccounts);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (metrics != null) {
            serializer.writeList($SCHEMA_METRICS, metrics, metrics.size(), SharedSerde.MetricsSerializer.INSTANCE);
        }
        if (nextToken != null) {
            serializer.writeString($SCHEMA_NEXT_TOKEN, nextToken);
        }
        if (owningAccounts != null) {
            serializer.writeList($SCHEMA_OWNING_ACCOUNTS, owningAccounts, owningAccounts.size(), SharedSerde.OwningAccountsSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRICS, member, metrics);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_OWNING_ACCOUNTS, member, owningAccounts);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListMetricsOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.metrics(this.metrics);
        builder.nextToken(this.nextToken);
        builder.owningAccounts(this.owningAccounts);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListMetricsOutput}.
     */
    public static final class Builder implements ShapeBuilder<ListMetricsOutput> {
        private List<Metric> metrics;
        private String nextToken;
        private List<String> owningAccounts;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The metrics that match your request.
         *
         * @return this builder.
         */
        public Builder metrics(List<Metric> metrics) {
            this.metrics = metrics;
            return this;
        }

        /**
         * The token that marks the start of the next batch of returned results.
         *
         * @return this builder.
         */
        public Builder nextToken(String nextToken) {
            this.nextToken = nextToken;
            return this;
        }

        /**
         * If you are using this operation in a monitoring account, this array contains the account IDs of the source
         * accounts where the metrics in the returned data are from.
         *
         * <p>This field is a 1:1 mapping between each metric that is returned and the ID of the owning account.
         *
         * @return this builder.
         */
        public Builder owningAccounts(List<String> owningAccounts) {
            this.owningAccounts = owningAccounts;
            return this;
        }

        @Override
        public ListMetricsOutput build() {
            return new ListMetricsOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> metrics((List<Metric>) SchemaUtils.validateSameMember($SCHEMA_METRICS, member, value));
                case 1 -> nextToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, value));
                case 2 -> owningAccounts((List<String>) SchemaUtils.validateSameMember($SCHEMA_OWNING_ACCOUNTS, member, value));
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
                    case 0 -> builder.metrics(SharedSerde.deserializeMetrics(member, de));
                    case 1 -> builder.nextToken(de.readString(member));
                    case 2 -> builder.owningAccounts(SharedSerde.deserializeOwningAccounts(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
