package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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

/**
 * Represents the properties of a replica global secondary index.
 */
@SmithyGenerated
public final class ReplicaGlobalSecondaryIndexDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_DESCRIPTION;
    private static final Schema $SCHEMA_INDEX_NAME = $SCHEMA.member("IndexName");
    private static final Schema $SCHEMA_PROVISIONED_THROUGHPUT_OVERRIDE = $SCHEMA.member("ProvisionedThroughputOverride");
    private static final Schema $SCHEMA_ON_DEMAND_THROUGHPUT_OVERRIDE = $SCHEMA.member("OnDemandThroughputOverride");
    private static final Schema $SCHEMA_WARM_THROUGHPUT = $SCHEMA.member("WarmThroughput");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String indexName;
    private final transient ProvisionedThroughputOverride provisionedThroughputOverride;
    private final transient OnDemandThroughputOverride onDemandThroughputOverride;
    private final transient GlobalSecondaryIndexWarmThroughputDescription warmThroughput;

    private ReplicaGlobalSecondaryIndexDescription(Builder builder) {
        this.indexName = builder.indexName;
        this.provisionedThroughputOverride = builder.provisionedThroughputOverride;
        this.onDemandThroughputOverride = builder.onDemandThroughputOverride;
        this.warmThroughput = builder.warmThroughput;
    }

    /**
     * The name of the global secondary index.
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * If not described, uses the source table GSI's read capacity settings.
     */
    public ProvisionedThroughputOverride getProvisionedThroughputOverride() {
        return provisionedThroughputOverride;
    }

    /**
     * Overrides the maximum on-demand throughput for the specified global secondary index in the specified replica
     * table.
     */
    public OnDemandThroughputOverride getOnDemandThroughputOverride() {
        return onDemandThroughputOverride;
    }

    /**
     * Represents the warm throughput of the global secondary index for this replica.
     */
    public GlobalSecondaryIndexWarmThroughputDescription getWarmThroughput() {
        return warmThroughput;
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
        ReplicaGlobalSecondaryIndexDescription that = (ReplicaGlobalSecondaryIndexDescription) other;
        return Objects.equals(this.indexName, that.indexName)
               && Objects.equals(this.provisionedThroughputOverride, that.provisionedThroughputOverride)
               && Objects.equals(this.onDemandThroughputOverride, that.onDemandThroughputOverride)
               && Objects.equals(this.warmThroughput, that.warmThroughput);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(indexName);
        $hc = 31 * $hc + Objects.hashCode(provisionedThroughputOverride);
        $hc = 31 * $hc + Objects.hashCode(onDemandThroughputOverride);
        $hc = 31 * $hc + Objects.hashCode(warmThroughput);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (indexName != null) {
            serializer.writeString($SCHEMA_INDEX_NAME, indexName);
        }
        if (provisionedThroughputOverride != null) {
            serializer.writeStruct($SCHEMA_PROVISIONED_THROUGHPUT_OVERRIDE, provisionedThroughputOverride);
        }
        if (onDemandThroughputOverride != null) {
            serializer.writeStruct($SCHEMA_ON_DEMAND_THROUGHPUT_OVERRIDE, onDemandThroughputOverride);
        }
        if (warmThroughput != null) {
            serializer.writeStruct($SCHEMA_WARM_THROUGHPUT, warmThroughput);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, indexName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_THROUGHPUT_OVERRIDE, member, provisionedThroughputOverride);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_ON_DEMAND_THROUGHPUT_OVERRIDE, member, onDemandThroughputOverride);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_WARM_THROUGHPUT, member, warmThroughput);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ReplicaGlobalSecondaryIndexDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.indexName(this.indexName);
        builder.provisionedThroughputOverride(this.provisionedThroughputOverride);
        builder.onDemandThroughputOverride(this.onDemandThroughputOverride);
        builder.warmThroughput(this.warmThroughput);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ReplicaGlobalSecondaryIndexDescription}.
     */
    public static final class Builder implements ShapeBuilder<ReplicaGlobalSecondaryIndexDescription> {
        private String indexName;
        private ProvisionedThroughputOverride provisionedThroughputOverride;
        private OnDemandThroughputOverride onDemandThroughputOverride;
        private GlobalSecondaryIndexWarmThroughputDescription warmThroughput;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the global secondary index.
         *
         * @return this builder.
         */
        public Builder indexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        /**
         * If not described, uses the source table GSI's read capacity settings.
         *
         * @return this builder.
         */
        public Builder provisionedThroughputOverride(ProvisionedThroughputOverride provisionedThroughputOverride) {
            this.provisionedThroughputOverride = provisionedThroughputOverride;
            return this;
        }

        /**
         * Overrides the maximum on-demand throughput for the specified global secondary index in the specified replica
         * table.
         *
         * @return this builder.
         */
        public Builder onDemandThroughputOverride(OnDemandThroughputOverride onDemandThroughputOverride) {
            this.onDemandThroughputOverride = onDemandThroughputOverride;
            return this;
        }

        /**
         * Represents the warm throughput of the global secondary index for this replica.
         *
         * @return this builder.
         */
        public Builder warmThroughput(GlobalSecondaryIndexWarmThroughputDescription warmThroughput) {
            this.warmThroughput = warmThroughput;
            return this;
        }

        @Override
        public ReplicaGlobalSecondaryIndexDescription build() {
            return new ReplicaGlobalSecondaryIndexDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> indexName((String) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, value));
                case 1 -> provisionedThroughputOverride((ProvisionedThroughputOverride) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_THROUGHPUT_OVERRIDE, member, value));
                case 2 -> onDemandThroughputOverride((OnDemandThroughputOverride) SchemaUtils.validateSameMember($SCHEMA_ON_DEMAND_THROUGHPUT_OVERRIDE, member, value));
                case 3 -> warmThroughput((GlobalSecondaryIndexWarmThroughputDescription) SchemaUtils.validateSameMember($SCHEMA_WARM_THROUGHPUT, member, value));
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
                    case 0 -> builder.indexName(de.readString(member));
                    case 1 -> builder.provisionedThroughputOverride(ProvisionedThroughputOverride.builder().deserializeMember(de, member).build());
                    case 2 -> builder.onDemandThroughputOverride(OnDemandThroughputOverride.builder().deserializeMember(de, member).build());
                    case 3 -> builder.warmThroughput(GlobalSecondaryIndexWarmThroughputDescription.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
