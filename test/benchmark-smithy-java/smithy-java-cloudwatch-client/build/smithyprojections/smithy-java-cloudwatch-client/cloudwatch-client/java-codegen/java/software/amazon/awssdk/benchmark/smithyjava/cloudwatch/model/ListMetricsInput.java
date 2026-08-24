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
public final class ListMetricsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.LIST_METRICS_INPUT;
    private static final Schema $SCHEMA_NAMESPACE = $SCHEMA.member("Namespace");
    private static final Schema $SCHEMA_METRIC_NAME = $SCHEMA.member("MetricName");
    private static final Schema $SCHEMA_DIMENSIONS = $SCHEMA.member("Dimensions");
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");
    private static final Schema $SCHEMA_RECENTLY_ACTIVE = $SCHEMA.member("RecentlyActive");
    private static final Schema $SCHEMA_INCLUDE_LINKED_ACCOUNTS = $SCHEMA.member("IncludeLinkedAccounts");
    private static final Schema $SCHEMA_OWNING_ACCOUNT = $SCHEMA.member("OwningAccount");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String namespace;
    private final transient String metricName;
    private final transient List<DimensionFilter> dimensions;
    private final transient String nextToken;
    private final transient RecentlyActive recentlyActive;
    private final transient Boolean includeLinkedAccounts;
    private final transient String owningAccount;

    private ListMetricsInput(Builder builder) {
        this.namespace = builder.namespace;
        this.metricName = builder.metricName;
        this.dimensions = builder.dimensions == null ? null : Collections.unmodifiableList(builder.dimensions);
        this.nextToken = builder.nextToken;
        this.recentlyActive = builder.recentlyActive;
        this.includeLinkedAccounts = builder.includeLinkedAccounts;
        this.owningAccount = builder.owningAccount;
    }

    /**
     * The metric namespace to filter against. Only the namespace that matches exactly will be returned.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * The name of the metric to filter against. Only the metrics with names that match exactly will be returned.
     */
    public String getMetricName() {
        return metricName;
    }

    /**
     * The dimensions to filter against. Only the dimension with names that match exactly will be returned. If you
     * specify one dimension name and a metric has that dimension and also other dimensions, it will be returned.
     */
    public List<DimensionFilter> getDimensions() {
        if (dimensions == null) {
            return Collections.emptyList();
        }
        return dimensions;
    }

    public boolean hasDimensions() {
        return dimensions != null;
    }

    /**
     * The token returned by a previous call to indicate that there is more data available.
     */
    public String getNextToken() {
        return nextToken;
    }

    /**
     * To filter the results to show only metrics that have had data points published in the past three hours, specify
     * this parameter with a value of <code>PT3H</code>. This is the only valid value for this parameter.
     *
     * <p>The results that are returned are an approximation of the value you specify. There is a low probability that
     * the returned results include metrics with last published data as much as 50 minutes more than the specified time
     * interval.
     */
    public RecentlyActive getRecentlyActive() {
        return recentlyActive;
    }

    /**
     * If you are using this operation in a monitoring account, specify <code>true</code> to include metrics from source
     * accounts in the returned data.
     *
     * <p>The default is <code>false</code>.
     */
    public Boolean isIncludeLinkedAccounts() {
        return includeLinkedAccounts;
    }

    /**
     * When you use this operation in a monitoring account, use this field to return metrics only from one source
     * account. To do so, specify that source account ID in this field, and also specify <code>true</code> for
     * <code>IncludeLinkedAccounts</code>.
     */
    public String getOwningAccount() {
        return owningAccount;
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
        ListMetricsInput that = (ListMetricsInput) other;
        return Objects.equals(this.includeLinkedAccounts, that.includeLinkedAccounts)
               && Objects.equals(this.namespace, that.namespace)
               && Objects.equals(this.metricName, that.metricName)
               && Objects.equals(this.nextToken, that.nextToken)
               && Objects.equals(this.owningAccount, that.owningAccount)
               && Objects.equals(this.recentlyActive, that.recentlyActive)
               && Objects.equals(this.dimensions, that.dimensions);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(namespace);
        $hc = 31 * $hc + Objects.hashCode(metricName);
        $hc = 31 * $hc + Objects.hashCode(dimensions);
        $hc = 31 * $hc + Objects.hashCode(nextToken);
        $hc = 31 * $hc + Objects.hashCode(recentlyActive);
        $hc = 31 * $hc + Objects.hashCode(includeLinkedAccounts);
        $hc = 31 * $hc + Objects.hashCode(owningAccount);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (namespace != null) {
            serializer.writeString($SCHEMA_NAMESPACE, namespace);
        }
        if (metricName != null) {
            serializer.writeString($SCHEMA_METRIC_NAME, metricName);
        }
        if (dimensions != null) {
            serializer.writeList($SCHEMA_DIMENSIONS, dimensions, dimensions.size(), SharedSerde.DimensionFiltersSerializer.INSTANCE);
        }
        if (nextToken != null) {
            serializer.writeString($SCHEMA_NEXT_TOKEN, nextToken);
        }
        if (recentlyActive != null) {
            serializer.writeString($SCHEMA_RECENTLY_ACTIVE, recentlyActive.getValue());
        }
        if (includeLinkedAccounts != null) {
            serializer.writeBoolean($SCHEMA_INCLUDE_LINKED_ACCOUNTS, includeLinkedAccounts);
        }
        if (owningAccount != null) {
            serializer.writeString($SCHEMA_OWNING_ACCOUNT, owningAccount);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_NAMESPACE, member, namespace);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_METRIC_NAME, member, metricName);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, dimensions);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_RECENTLY_ACTIVE, member, recentlyActive);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_INCLUDE_LINKED_ACCOUNTS, member, includeLinkedAccounts);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_OWNING_ACCOUNT, member, owningAccount);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListMetricsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.namespace(this.namespace);
        builder.metricName(this.metricName);
        builder.dimensions(this.dimensions);
        builder.nextToken(this.nextToken);
        builder.recentlyActive(this.recentlyActive);
        builder.includeLinkedAccounts(this.includeLinkedAccounts);
        builder.owningAccount(this.owningAccount);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListMetricsInput}.
     */
    public static final class Builder implements ShapeBuilder<ListMetricsInput> {
        private String namespace;
        private String metricName;
        private List<DimensionFilter> dimensions;
        private String nextToken;
        private RecentlyActive recentlyActive;
        private Boolean includeLinkedAccounts;
        private String owningAccount;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The metric namespace to filter against. Only the namespace that matches exactly will be returned.
         *
         * @return this builder.
         */
        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * The name of the metric to filter against. Only the metrics with names that match exactly will be returned.
         *
         * @return this builder.
         */
        public Builder metricName(String metricName) {
            this.metricName = metricName;
            return this;
        }

        /**
         * The dimensions to filter against. Only the dimension with names that match exactly will be returned. If you
         * specify one dimension name and a metric has that dimension and also other dimensions, it will be returned.
         *
         * @return this builder.
         */
        public Builder dimensions(List<DimensionFilter> dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        /**
         * The token returned by a previous call to indicate that there is more data available.
         *
         * @return this builder.
         */
        public Builder nextToken(String nextToken) {
            this.nextToken = nextToken;
            return this;
        }

        /**
         * To filter the results to show only metrics that have had data points published in the past three hours, specify
         * this parameter with a value of <code>PT3H</code>. This is the only valid value for this parameter.
         *
         * <p>The results that are returned are an approximation of the value you specify. There is a low probability that
         * the returned results include metrics with last published data as much as 50 minutes more than the specified time
         * interval.
         *
         * @return this builder.
         */
        public Builder recentlyActive(RecentlyActive recentlyActive) {
            this.recentlyActive = recentlyActive;
            return this;
        }

        /**
         * If you are using this operation in a monitoring account, specify <code>true</code> to include metrics from source
         * accounts in the returned data.
         *
         * <p>The default is <code>false</code>.
         *
         * @return this builder.
         */
        public Builder includeLinkedAccounts(Boolean includeLinkedAccounts) {
            this.includeLinkedAccounts = includeLinkedAccounts;
            return this;
        }

        /**
         * When you use this operation in a monitoring account, use this field to return metrics only from one source
         * account. To do so, specify that source account ID in this field, and also specify <code>true</code> for
         * <code>IncludeLinkedAccounts</code>.
         *
         * @return this builder.
         */
        public Builder owningAccount(String owningAccount) {
            this.owningAccount = owningAccount;
            return this;
        }

        @Override
        public ListMetricsInput build() {
            return new ListMetricsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> namespace((String) SchemaUtils.validateSameMember($SCHEMA_NAMESPACE, member, value));
                case 1 -> metricName((String) SchemaUtils.validateSameMember($SCHEMA_METRIC_NAME, member, value));
                case 2 -> dimensions((List<DimensionFilter>) SchemaUtils.validateSameMember($SCHEMA_DIMENSIONS, member, value));
                case 3 -> nextToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, value));
                case 4 -> recentlyActive((RecentlyActive) SchemaUtils.validateSameMember($SCHEMA_RECENTLY_ACTIVE, member, value));
                case 5 -> includeLinkedAccounts((Boolean) SchemaUtils.validateSameMember($SCHEMA_INCLUDE_LINKED_ACCOUNTS, member, value));
                case 6 -> owningAccount((String) SchemaUtils.validateSameMember($SCHEMA_OWNING_ACCOUNT, member, value));
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
                    case 0 -> builder.namespace(de.readString(member));
                    case 1 -> builder.metricName(de.readString(member));
                    case 2 -> builder.dimensions(SharedSerde.deserializeDimensionFilters(member, de));
                    case 3 -> builder.nextToken(de.readString(member));
                    case 4 -> builder.recentlyActive(RecentlyActive.builder().deserializeMember(de, member).build());
                    case 5 -> builder.includeLinkedAccounts(de.readBoolean(member));
                    case 6 -> builder.owningAccount(de.readString(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
