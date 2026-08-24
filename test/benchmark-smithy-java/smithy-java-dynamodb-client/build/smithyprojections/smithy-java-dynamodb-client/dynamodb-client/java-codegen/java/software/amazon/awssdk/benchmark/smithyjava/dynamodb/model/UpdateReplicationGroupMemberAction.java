package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
 * Represents a replica to be modified.
 */
@SmithyGenerated
public final class UpdateReplicationGroupMemberAction implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.UPDATE_REPLICATION_GROUP_MEMBER_ACTION;
    private static final Schema $SCHEMA_REGION_NAME = $SCHEMA.member("RegionName");
    private static final Schema $SCHEMA_KMS_MASTER_KEY_ID = $SCHEMA.member("KMSMasterKeyId");
    private static final Schema $SCHEMA_PROVISIONED_THROUGHPUT_OVERRIDE = $SCHEMA.member("ProvisionedThroughputOverride");
    private static final Schema $SCHEMA_ON_DEMAND_THROUGHPUT_OVERRIDE = $SCHEMA.member("OnDemandThroughputOverride");
    private static final Schema $SCHEMA_GLOBAL_SECONDARY_INDEXES = $SCHEMA.member("GlobalSecondaryIndexes");
    private static final Schema $SCHEMA_TABLE_CLASS_OVERRIDE = $SCHEMA.member("TableClassOverride");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String regionName;
    private final transient String kmsMasterKeyId;
    private final transient ProvisionedThroughputOverride provisionedThroughputOverride;
    private final transient OnDemandThroughputOverride onDemandThroughputOverride;
    private final transient List<ReplicaGlobalSecondaryIndex> globalSecondaryIndexes;
    private final transient TableClass tableClassOverride;

    private UpdateReplicationGroupMemberAction(Builder builder) {
        this.regionName = builder.regionName;
        this.kmsMasterKeyId = builder.kmsMasterKeyId;
        this.provisionedThroughputOverride = builder.provisionedThroughputOverride;
        this.onDemandThroughputOverride = builder.onDemandThroughputOverride;
        this.globalSecondaryIndexes = builder.globalSecondaryIndexes == null ? null : Collections.unmodifiableList(builder.globalSecondaryIndexes);
        this.tableClassOverride = builder.tableClassOverride;
    }

    /**
     * The Region where the replica exists.
     */
    public String getRegionName() {
        return regionName;
    }

    /**
     * The KMS key of the replica that should be used for KMS encryption. To specify a key, use its key ID, Amazon
     * Resource Name (ARN), alias name, or alias ARN. Note that you should only provide this parameter if the key is
     * different from the default DynamoDB KMS key <code>alias/aws/dynamodb</code>.
     */
    public String getKmsMasterKeyId() {
        return kmsMasterKeyId;
    }

    /**
     * Replica-specific provisioned throughput. If not specified, uses the source table's provisioned throughput
     * settings.
     */
    public ProvisionedThroughputOverride getProvisionedThroughputOverride() {
        return provisionedThroughputOverride;
    }

    /**
     * Overrides the maximum on-demand throughput for the replica table.
     */
    public OnDemandThroughputOverride getOnDemandThroughputOverride() {
        return onDemandThroughputOverride;
    }

    /**
     * Replica-specific global secondary index settings.
     */
    public List<ReplicaGlobalSecondaryIndex> getGlobalSecondaryIndexes() {
        if (globalSecondaryIndexes == null) {
            return Collections.emptyList();
        }
        return globalSecondaryIndexes;
    }

    public boolean hasGlobalSecondaryIndexes() {
        return globalSecondaryIndexes != null;
    }

    /**
     * Replica-specific table class. If not specified, uses the source table's table class.
     */
    public TableClass getTableClassOverride() {
        return tableClassOverride;
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
        UpdateReplicationGroupMemberAction that = (UpdateReplicationGroupMemberAction) other;
        return Objects.equals(this.regionName, that.regionName)
               && Objects.equals(this.kmsMasterKeyId, that.kmsMasterKeyId)
               && Objects.equals(this.tableClassOverride, that.tableClassOverride)
               && Objects.equals(this.provisionedThroughputOverride, that.provisionedThroughputOverride)
               && Objects.equals(this.onDemandThroughputOverride, that.onDemandThroughputOverride)
               && Objects.equals(this.globalSecondaryIndexes, that.globalSecondaryIndexes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(regionName);
        $hc = 31 * $hc + Objects.hashCode(kmsMasterKeyId);
        $hc = 31 * $hc + Objects.hashCode(provisionedThroughputOverride);
        $hc = 31 * $hc + Objects.hashCode(onDemandThroughputOverride);
        $hc = 31 * $hc + Objects.hashCode(globalSecondaryIndexes);
        $hc = 31 * $hc + Objects.hashCode(tableClassOverride);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_REGION_NAME, regionName);
        if (kmsMasterKeyId != null) {
            serializer.writeString($SCHEMA_KMS_MASTER_KEY_ID, kmsMasterKeyId);
        }
        if (provisionedThroughputOverride != null) {
            serializer.writeStruct($SCHEMA_PROVISIONED_THROUGHPUT_OVERRIDE, provisionedThroughputOverride);
        }
        if (onDemandThroughputOverride != null) {
            serializer.writeStruct($SCHEMA_ON_DEMAND_THROUGHPUT_OVERRIDE, onDemandThroughputOverride);
        }
        if (globalSecondaryIndexes != null) {
            serializer.writeList($SCHEMA_GLOBAL_SECONDARY_INDEXES, globalSecondaryIndexes, globalSecondaryIndexes.size(), SharedSerde.ReplicaGlobalSecondaryIndexListSerializer.INSTANCE);
        }
        if (tableClassOverride != null) {
            serializer.writeString($SCHEMA_TABLE_CLASS_OVERRIDE, tableClassOverride.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_REGION_NAME, member, regionName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_KMS_MASTER_KEY_ID, member, kmsMasterKeyId);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_THROUGHPUT_OVERRIDE, member, provisionedThroughputOverride);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_ON_DEMAND_THROUGHPUT_OVERRIDE, member, onDemandThroughputOverride);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_SECONDARY_INDEXES, member, globalSecondaryIndexes);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_CLASS_OVERRIDE, member, tableClassOverride);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link UpdateReplicationGroupMemberAction}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.regionName(this.regionName);
        builder.kmsMasterKeyId(this.kmsMasterKeyId);
        builder.provisionedThroughputOverride(this.provisionedThroughputOverride);
        builder.onDemandThroughputOverride(this.onDemandThroughputOverride);
        builder.globalSecondaryIndexes(this.globalSecondaryIndexes);
        builder.tableClassOverride(this.tableClassOverride);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link UpdateReplicationGroupMemberAction}.
     */
    public static final class Builder implements ShapeBuilder<UpdateReplicationGroupMemberAction> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String regionName;
        private String kmsMasterKeyId;
        private ProvisionedThroughputOverride provisionedThroughputOverride;
        private OnDemandThroughputOverride onDemandThroughputOverride;
        private List<ReplicaGlobalSecondaryIndex> globalSecondaryIndexes;
        private TableClass tableClassOverride;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The Region where the replica exists.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder regionName(String regionName) {
            this.regionName = Objects.requireNonNull(regionName, "regionName cannot be null");
            tracker.setMember($SCHEMA_REGION_NAME);
            return this;
        }

        /**
         * The KMS key of the replica that should be used for KMS encryption. To specify a key, use its key ID, Amazon
         * Resource Name (ARN), alias name, or alias ARN. Note that you should only provide this parameter if the key is
         * different from the default DynamoDB KMS key <code>alias/aws/dynamodb</code>.
         *
         * @return this builder.
         */
        public Builder kmsMasterKeyId(String kmsMasterKeyId) {
            this.kmsMasterKeyId = kmsMasterKeyId;
            return this;
        }

        /**
         * Replica-specific provisioned throughput. If not specified, uses the source table's provisioned throughput
         * settings.
         *
         * @return this builder.
         */
        public Builder provisionedThroughputOverride(ProvisionedThroughputOverride provisionedThroughputOverride) {
            this.provisionedThroughputOverride = provisionedThroughputOverride;
            return this;
        }

        /**
         * Overrides the maximum on-demand throughput for the replica table.
         *
         * @return this builder.
         */
        public Builder onDemandThroughputOverride(OnDemandThroughputOverride onDemandThroughputOverride) {
            this.onDemandThroughputOverride = onDemandThroughputOverride;
            return this;
        }

        /**
         * Replica-specific global secondary index settings.
         *
         * @return this builder.
         */
        public Builder globalSecondaryIndexes(List<ReplicaGlobalSecondaryIndex> globalSecondaryIndexes) {
            this.globalSecondaryIndexes = globalSecondaryIndexes;
            return this;
        }

        /**
         * Replica-specific table class. If not specified, uses the source table's table class.
         *
         * @return this builder.
         */
        public Builder tableClassOverride(TableClass tableClassOverride) {
            this.tableClassOverride = tableClassOverride;
            return this;
        }

        @Override
        public UpdateReplicationGroupMemberAction build() {
            tracker.validate();
            return new UpdateReplicationGroupMemberAction(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> regionName((String) SchemaUtils.validateSameMember($SCHEMA_REGION_NAME, member, value));
                case 1 -> kmsMasterKeyId((String) SchemaUtils.validateSameMember($SCHEMA_KMS_MASTER_KEY_ID, member, value));
                case 2 -> provisionedThroughputOverride((ProvisionedThroughputOverride) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_THROUGHPUT_OVERRIDE, member, value));
                case 3 -> onDemandThroughputOverride((OnDemandThroughputOverride) SchemaUtils.validateSameMember($SCHEMA_ON_DEMAND_THROUGHPUT_OVERRIDE, member, value));
                case 4 -> globalSecondaryIndexes((List<ReplicaGlobalSecondaryIndex>) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_SECONDARY_INDEXES, member, value));
                case 5 -> tableClassOverride((TableClass) SchemaUtils.validateSameMember($SCHEMA_TABLE_CLASS_OVERRIDE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<UpdateReplicationGroupMemberAction> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_REGION_NAME)) {
                regionName("");
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
                    case 0 -> builder.regionName(de.readString(member));
                    case 1 -> builder.kmsMasterKeyId(de.readString(member));
                    case 2 -> builder.provisionedThroughputOverride(ProvisionedThroughputOverride.builder().deserializeMember(de, member).build());
                    case 3 -> builder.onDemandThroughputOverride(OnDemandThroughputOverride.builder().deserializeMember(de, member).build());
                    case 4 -> builder.globalSecondaryIndexes(SharedSerde.deserializeReplicaGlobalSecondaryIndexList(member, de));
                    case 5 -> builder.tableClassOverride(TableClass.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
