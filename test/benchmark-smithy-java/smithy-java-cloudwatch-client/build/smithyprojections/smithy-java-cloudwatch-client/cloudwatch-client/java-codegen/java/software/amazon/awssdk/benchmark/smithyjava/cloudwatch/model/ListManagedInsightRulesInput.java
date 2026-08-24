package software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model;

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

@SmithyGenerated
public final class ListManagedInsightRulesInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.LIST_MANAGED_INSIGHT_RULES_INPUT;
    private static final Schema $SCHEMA_RESOURCEAR_N = $SCHEMA.member("ResourceARN");
    private static final Schema $SCHEMA_NEXT_TOKEN = $SCHEMA.member("NextToken");
    private static final Schema $SCHEMA_MAX_RESULTS = $SCHEMA.member("MaxResults");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String resourcearN;
    private final transient String nextToken;
    private final transient Integer maxResults;

    private ListManagedInsightRulesInput(Builder builder) {
        this.resourcearN = builder.resourcearN;
        this.nextToken = builder.nextToken;
        this.maxResults = builder.maxResults;
    }

    /**
     * The ARN of an Amazon Web Services resource that has managed Contributor Insights rules.
     */
    public String getResourcearN() {
        return resourcearN;
    }

    /**
     * Include this value to get the next set of rules if the value was returned by the previous operation.
     */
    public String getNextToken() {
        return nextToken;
    }

    /**
     * The maximum number of results to return in one operation. If you omit this parameter, the default number is used.
     * The default number is <code>100</code>.
     */
    public Integer getMaxResults() {
        return maxResults;
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
        ListManagedInsightRulesInput that = (ListManagedInsightRulesInput) other;
        return Objects.equals(this.maxResults, that.maxResults)
               && Objects.equals(this.resourcearN, that.resourcearN)
               && Objects.equals(this.nextToken, that.nextToken);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(resourcearN);
        $hc = 31 * $hc + Objects.hashCode(nextToken);
        $hc = 31 * $hc + Objects.hashCode(maxResults);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (resourcearN != null) {
            serializer.writeString($SCHEMA_RESOURCEAR_N, resourcearN);
        }
        if (nextToken != null) {
            serializer.writeString($SCHEMA_NEXT_TOKEN, nextToken);
        }
        if (maxResults != null) {
            serializer.writeInteger($SCHEMA_MAX_RESULTS, maxResults);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_RESOURCEAR_N, member, resourcearN);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, nextToken);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_MAX_RESULTS, member, maxResults);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ListManagedInsightRulesInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.resourcearN(this.resourcearN);
        builder.nextToken(this.nextToken);
        builder.maxResults(this.maxResults);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListManagedInsightRulesInput}.
     */
    public static final class Builder implements ShapeBuilder<ListManagedInsightRulesInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String resourcearN;
        private String nextToken;
        private Integer maxResults;

        private Builder() {
            // Tell the tracker to assume clientOptional members are present.
            tracker.setMember($SCHEMA_RESOURCEAR_N);
        }

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The ARN of an Amazon Web Services resource that has managed Contributor Insights rules.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder resourcearN(String resourcearN) {
            this.resourcearN = Objects.requireNonNull(resourcearN, "resourcearN cannot be null");
            tracker.setMember($SCHEMA_RESOURCEAR_N);
            return this;
        }

        /**
         * Include this value to get the next set of rules if the value was returned by the previous operation.
         *
         * @return this builder.
         */
        public Builder nextToken(String nextToken) {
            this.nextToken = nextToken;
            return this;
        }

        /**
         * The maximum number of results to return in one operation. If you omit this parameter, the default number is used.
         * The default number is <code>100</code>.
         *
         * @return this builder.
         */
        public Builder maxResults(Integer maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        @Override
        public ListManagedInsightRulesInput build() {
            tracker.validate();
            return new ListManagedInsightRulesInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> resourcearN((String) SchemaUtils.validateSameMember($SCHEMA_RESOURCEAR_N, member, value));
                case 1 -> nextToken((String) SchemaUtils.validateSameMember($SCHEMA_NEXT_TOKEN, member, value));
                case 2 -> maxResults((Integer) SchemaUtils.validateSameMember($SCHEMA_MAX_RESULTS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ListManagedInsightRulesInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_RESOURCEAR_N)) {
                resourcearN("");
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
                    case 0 -> builder.resourcearN(de.readString(member));
                    case 1 -> builder.nextToken(de.readString(member));
                    case 2 -> builder.maxResults(de.readInteger(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
