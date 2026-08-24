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
 * Represents the input of an <code>UpdateTable</code> operation.
 */
@SmithyGenerated
public final class UpdateTableInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.UPDATE_TABLE_INPUT;
    private static final Schema $SCHEMA_ATTRIBUTE_DEFINITIONS = $SCHEMA.member("AttributeDefinitions");
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_BILLING_MODE = $SCHEMA.member("BillingMode");
    private static final Schema $SCHEMA_PROVISIONED_THROUGHPUT = $SCHEMA.member("ProvisionedThroughput");
    private static final Schema $SCHEMA_GLOBAL_SECONDARY_INDEX_UPDATES = $SCHEMA.member("GlobalSecondaryIndexUpdates");
    private static final Schema $SCHEMA_STREAM_SPECIFICATION = $SCHEMA.member("StreamSpecification");
    private static final Schema $SCHEMA_SSE_SPECIFICATION = $SCHEMA.member("SSESpecification");
    private static final Schema $SCHEMA_REPLICA_UPDATES = $SCHEMA.member("ReplicaUpdates");
    private static final Schema $SCHEMA_TABLE_CLASS = $SCHEMA.member("TableClass");
    private static final Schema $SCHEMA_DELETION_PROTECTION_ENABLED = $SCHEMA.member("DeletionProtectionEnabled");
    private static final Schema $SCHEMA_MULTI_REGION_CONSISTENCY = $SCHEMA.member("MultiRegionConsistency");
    private static final Schema $SCHEMA_GLOBAL_TABLE_WITNESS_UPDATES = $SCHEMA.member("GlobalTableWitnessUpdates");
    private static final Schema $SCHEMA_ON_DEMAND_THROUGHPUT = $SCHEMA.member("OnDemandThroughput");
    private static final Schema $SCHEMA_WARM_THROUGHPUT = $SCHEMA.member("WarmThroughput");
    private static final Schema $SCHEMA_GLOBAL_TABLE_SETTINGS_REPLICATION_MODE = $SCHEMA.member("GlobalTableSettingsReplicationMode");
    private static final Schema $SCHEMA_VECTOR_INDEX_UPDATES = $SCHEMA.member("VectorIndexUpdates");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<AttributeDefinition> attributeDefinitions;
    private final transient String tableName;
    private final transient BillingMode billingMode;
    private final transient ProvisionedThroughput provisionedThroughput;
    private final transient List<GlobalSecondaryIndexUpdate> globalSecondaryIndexUpdates;
    private final transient StreamSpecification streamSpecification;
    private final transient SSESpecification sseSpecification;
    private final transient List<ReplicationGroupUpdate> replicaUpdates;
    private final transient TableClass tableClass;
    private final transient Boolean deletionProtectionEnabled;
    private final transient MultiRegionConsistency multiRegionConsistency;
    private final transient List<GlobalTableWitnessGroupUpdate> globalTableWitnessUpdates;
    private final transient OnDemandThroughput onDemandThroughput;
    private final transient WarmThroughput warmThroughput;
    private final transient GlobalTableSettingsReplicationMode globalTableSettingsReplicationMode;
    private final transient List<VectorIndexUpdate> vectorIndexUpdates;

    private UpdateTableInput(Builder builder) {
        this.attributeDefinitions = builder.attributeDefinitions == null ? null : Collections.unmodifiableList(builder.attributeDefinitions);
        this.tableName = builder.tableName;
        this.billingMode = builder.billingMode;
        this.provisionedThroughput = builder.provisionedThroughput;
        this.globalSecondaryIndexUpdates = builder.globalSecondaryIndexUpdates == null ? null : Collections.unmodifiableList(builder.globalSecondaryIndexUpdates);
        this.streamSpecification = builder.streamSpecification;
        this.sseSpecification = builder.sseSpecification;
        this.replicaUpdates = builder.replicaUpdates == null ? null : Collections.unmodifiableList(builder.replicaUpdates);
        this.tableClass = builder.tableClass;
        this.deletionProtectionEnabled = builder.deletionProtectionEnabled;
        this.multiRegionConsistency = builder.multiRegionConsistency;
        this.globalTableWitnessUpdates = builder.globalTableWitnessUpdates == null ? null : Collections.unmodifiableList(builder.globalTableWitnessUpdates);
        this.onDemandThroughput = builder.onDemandThroughput;
        this.warmThroughput = builder.warmThroughput;
        this.globalTableSettingsReplicationMode = builder.globalTableSettingsReplicationMode;
        this.vectorIndexUpdates = builder.vectorIndexUpdates == null ? null : Collections.unmodifiableList(builder.vectorIndexUpdates);
    }

    /**
     * An array of attributes that describe the key schema for the table and indexes. If you are adding a new global
     * secondary index to the table, <code>AttributeDefinitions</code> must include the key element(s) of the new index.
     */
    public List<AttributeDefinition> getAttributeDefinitions() {
        if (attributeDefinitions == null) {
            return Collections.emptyList();
        }
        return attributeDefinitions;
    }

    public boolean hasAttributeDefinitions() {
        return attributeDefinitions != null;
    }

    /**
     * The name of the table to be updated. You can also provide the Amazon Resource Name (ARN) of the table in this
     * parameter.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Controls how you are charged for read and write throughput and how you manage capacity. When switching from
     * pay-per-request to provisioned capacity, initial provisioned capacity values must be set. The initial provisioned
     * capacity values are estimated based on the consumed read and write capacity of your table and global secondary
     * indexes over the past 30 minutes.
     *
     * <ul>
     *   <li>
     *     <code>PAY_PER_REQUEST</code> - We recommend using <code>PAY_PER_REQUEST</code> for most DynamoDB
     *     workloads. <code>PAY_PER_REQUEST</code> sets the billing mode to <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/on-demand-capacity-mode.html">On-demand capacity mode</a>.
     *   </li>
     *   <li>
     *     <code>PROVISIONED</code> - We recommend using <code>PROVISIONED</code> for steady workloads with
     *     predictable growth where capacity requirements can be reliably forecasted. <code>PROVISIONED</code> sets
     *     the billing mode to <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/provisioned-capacity-mode.html">Provisioned capacity mode</a>.
     *   </li>
     * </ul>
     */
    public BillingMode getBillingMode() {
        return billingMode;
    }

    /**
     * The new provisioned throughput settings for the specified table or index.
     */
    public ProvisionedThroughput getProvisionedThroughput() {
        return provisionedThroughput;
    }

    /**
     * An array of one or more global secondary indexes for the table. For each index in the array, you can request one
     * action:
     *
     * <ul>
     *   <li>
     *     <code>Create</code> - add a new global secondary index to the table.
     *   </li>
     *   <li>
     *     <code>Update</code> - modify the provisioned throughput settings of an existing global secondary index.
     *   </li>
     *   <li>
     *     <code>Delete</code> - remove a global secondary index from the table.
     *   </li>
     * </ul>
     *
     * <p>You can create or delete only one global secondary index per <code>UpdateTable</code> operation.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GSI.OnlineOps.html">Managing Global Secondary Indexes</a> in the <i>Amazon DynamoDB Developer Guide</i>
     * .
     */
    public List<GlobalSecondaryIndexUpdate> getGlobalSecondaryIndexUpdates() {
        if (globalSecondaryIndexUpdates == null) {
            return Collections.emptyList();
        }
        return globalSecondaryIndexUpdates;
    }

    public boolean hasGlobalSecondaryIndexUpdates() {
        return globalSecondaryIndexUpdates != null;
    }

    /**
     * Represents the DynamoDB Streams configuration for the table.
     *
     * <p>You receive a <code>ValidationException</code> if you try to enable a stream on a table that already has a
     * stream, or if you try to disable a stream on a table that doesn't have a stream.
     */
    public StreamSpecification getStreamSpecification() {
        return streamSpecification;
    }

    /**
     * The new server-side encryption settings for the specified table.
     */
    public SSESpecification getSseSpecification() {
        return sseSpecification;
    }

    /**
     * A list of replica update actions (create, delete, or update) for the table.
     */
    public List<ReplicationGroupUpdate> getReplicaUpdates() {
        if (replicaUpdates == null) {
            return Collections.emptyList();
        }
        return replicaUpdates;
    }

    public boolean hasReplicaUpdates() {
        return replicaUpdates != null;
    }

    /**
     * The table class of the table to be updated. Valid values are <code>STANDARD</code> and
     * <code>STANDARD_INFREQUENT_ACCESS</code>.
     */
    public TableClass getTableClass() {
        return tableClass;
    }

    /**
     * Indicates whether deletion protection is to be enabled (true) or disabled (false) on the table.
     */
    public Boolean isDeletionProtectionEnabled() {
        return deletionProtectionEnabled;
    }

    /**
     * Specifies the consistency mode for a new global table. This parameter is only valid when you create a global
     * table by specifying one or more <a href="https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_ReplicationGroupUpdate.html#DDB-Type-ReplicationGroupUpdate-Create">Create</a> actions in the <a href="https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_UpdateTable.html#DDB-UpdateTable-request-ReplicaUpdates">ReplicaUpdates</a> action list.
     *
     * <p>You can specify one of the following consistency modes:
     *
     * <ul>
     *   <li>
     *     <code>EVENTUAL</code>: Configures a new global table for multi-Region eventual consistency (MREC). This
     *     is the default consistency mode for global tables.
     *   </li>
     *   <li>
     *     <code>STRONG</code>: Configures a new global table for multi-Region strong consistency (MRSC).
     *   </li>
     * </ul>
     *
     * <p>If you don't specify this field, the global table consistency mode defaults to <code>EVENTUAL</code>. For more
     * information about global tables consistency modes, see <a href="https://docs.aws.amazon.com/V2globaltables_HowItWorks.html#V2globaltables_HowItWorks.consistency-modes"> Consistency modes</a> in DynamoDB developer guide.
     */
    public MultiRegionConsistency getMultiRegionConsistency() {
        return multiRegionConsistency;
    }

    /**
     * A list of witness updates for a MRSC global table. A witness provides a cost-effective alternative to a full
     * replica in a MRSC global table by maintaining replicated change data written to global table replicas. You cannot
     * perform read or write operations on a witness. For each witness, you can request one action:
     *
     * <ul>
     *   <li>
     *     <code>Create</code> - add a new witness to the global table.
     *   </li>
     *   <li>
     *     <code>Delete</code> - remove a witness from the global table.
     *   </li>
     * </ul>
     *
     * <p>You can create or delete only one witness per <code>UpdateTable</code> operation.
     *
     * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/V2globaltables_HowItWorks.html#V2globaltables_HowItWorks.consistency-modes">Multi-Region strong consistency (MRSC)</a> in the Amazon DynamoDB Developer Guide
     */
    public List<GlobalTableWitnessGroupUpdate> getGlobalTableWitnessUpdates() {
        if (globalTableWitnessUpdates == null) {
            return Collections.emptyList();
        }
        return globalTableWitnessUpdates;
    }

    public boolean hasGlobalTableWitnessUpdates() {
        return globalTableWitnessUpdates != null;
    }

    /**
     * Updates the maximum number of read and write units for the specified table in on-demand capacity mode. If you use
     * this parameter, you must specify <code>MaxReadRequestUnits</code>, <code>MaxWriteRequestUnits</code>, or both.
     */
    public OnDemandThroughput getOnDemandThroughput() {
        return onDemandThroughput;
    }

    /**
     * Represents the warm throughput (in read units per second and write units per second) for updating a table.
     */
    public WarmThroughput getWarmThroughput() {
        return warmThroughput;
    }

    /**
     * Controls the settings replication mode for a global table replica. This attribute can be defined using
     * UpdateTable operation only on a regional table with values:
     *
     * <ul>
     *   <li>
     *     <code>ENABLED</code>: Defines settings replication on a regional table to be used as a source table for
     *     creating Multi-Account Global Table.
     *   </li>
     *   <li>
     *     <code>DISABLED</code>: Remove settings replication on a regional table. Settings replication needs to be
     *     defined to ENABLED again in order to create a Multi-Account Global Table using this table.
     *   </li>
     * </ul>
     */
    public GlobalTableSettingsReplicationMode getGlobalTableSettingsReplicationMode() {
        return globalTableSettingsReplicationMode;
    }

    /**
     * A list of vector indexes to be added to or removed from the table. You can add or remove one vector index for
     * each <code>UpdateTable</code> operation.
     *
     * <p>To add a vector index, specify <code>IndexName</code>, <code>VectorAttribute</code>, <code>Dimensions</code>, <code>
     * DistanceFunction</code>, and <code>Projection</code>. To remove a vector index, specify only the <code>IndexName</code>
     * .
     */
    public List<VectorIndexUpdate> getVectorIndexUpdates() {
        if (vectorIndexUpdates == null) {
            return Collections.emptyList();
        }
        return vectorIndexUpdates;
    }

    public boolean hasVectorIndexUpdates() {
        return vectorIndexUpdates != null;
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
        UpdateTableInput that = (UpdateTableInput) other;
        return Objects.equals(this.deletionProtectionEnabled, that.deletionProtectionEnabled)
               && Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.billingMode, that.billingMode)
               && Objects.equals(this.tableClass, that.tableClass)
               && Objects.equals(this.multiRegionConsistency, that.multiRegionConsistency)
               && Objects.equals(this.globalTableSettingsReplicationMode, that.globalTableSettingsReplicationMode)
               && Objects.equals(this.provisionedThroughput, that.provisionedThroughput)
               && Objects.equals(this.streamSpecification, that.streamSpecification)
               && Objects.equals(this.sseSpecification, that.sseSpecification)
               && Objects.equals(this.onDemandThroughput, that.onDemandThroughput)
               && Objects.equals(this.warmThroughput, that.warmThroughput)
               && Objects.equals(this.attributeDefinitions, that.attributeDefinitions)
               && Objects.equals(this.globalSecondaryIndexUpdates, that.globalSecondaryIndexUpdates)
               && Objects.equals(this.replicaUpdates, that.replicaUpdates)
               && Objects.equals(this.globalTableWitnessUpdates, that.globalTableWitnessUpdates)
               && Objects.equals(this.vectorIndexUpdates, that.vectorIndexUpdates);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(attributeDefinitions);
        $hc = 31 * $hc + Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(billingMode);
        $hc = 31 * $hc + Objects.hashCode(provisionedThroughput);
        $hc = 31 * $hc + Objects.hashCode(globalSecondaryIndexUpdates);
        $hc = 31 * $hc + Objects.hashCode(streamSpecification);
        $hc = 31 * $hc + Objects.hashCode(sseSpecification);
        $hc = 31 * $hc + Objects.hashCode(replicaUpdates);
        $hc = 31 * $hc + Objects.hashCode(tableClass);
        $hc = 31 * $hc + Objects.hashCode(deletionProtectionEnabled);
        $hc = 31 * $hc + Objects.hashCode(multiRegionConsistency);
        $hc = 31 * $hc + Objects.hashCode(globalTableWitnessUpdates);
        $hc = 31 * $hc + Objects.hashCode(onDemandThroughput);
        $hc = 31 * $hc + Objects.hashCode(warmThroughput);
        $hc = 31 * $hc + Objects.hashCode(globalTableSettingsReplicationMode);
        $hc = 31 * $hc + Objects.hashCode(vectorIndexUpdates);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (attributeDefinitions != null) {
            serializer.writeList($SCHEMA_ATTRIBUTE_DEFINITIONS, attributeDefinitions, attributeDefinitions.size(), SharedSerde.AttributeDefinitionsSerializer.INSTANCE);
        }
        serializer.writeString($SCHEMA_TABLE_NAME, tableName);
        if (billingMode != null) {
            serializer.writeString($SCHEMA_BILLING_MODE, billingMode.getValue());
        }
        if (provisionedThroughput != null) {
            serializer.writeStruct($SCHEMA_PROVISIONED_THROUGHPUT, provisionedThroughput);
        }
        if (globalSecondaryIndexUpdates != null) {
            serializer.writeList($SCHEMA_GLOBAL_SECONDARY_INDEX_UPDATES, globalSecondaryIndexUpdates, globalSecondaryIndexUpdates.size(), SharedSerde.GlobalSecondaryIndexUpdateListSerializer.INSTANCE);
        }
        if (streamSpecification != null) {
            serializer.writeStruct($SCHEMA_STREAM_SPECIFICATION, streamSpecification);
        }
        if (sseSpecification != null) {
            serializer.writeStruct($SCHEMA_SSE_SPECIFICATION, sseSpecification);
        }
        if (replicaUpdates != null) {
            serializer.writeList($SCHEMA_REPLICA_UPDATES, replicaUpdates, replicaUpdates.size(), SharedSerde.ReplicationGroupUpdateListSerializer.INSTANCE);
        }
        if (tableClass != null) {
            serializer.writeString($SCHEMA_TABLE_CLASS, tableClass.getValue());
        }
        if (deletionProtectionEnabled != null) {
            serializer.writeBoolean($SCHEMA_DELETION_PROTECTION_ENABLED, deletionProtectionEnabled);
        }
        if (multiRegionConsistency != null) {
            serializer.writeString($SCHEMA_MULTI_REGION_CONSISTENCY, multiRegionConsistency.getValue());
        }
        if (globalTableWitnessUpdates != null) {
            serializer.writeList($SCHEMA_GLOBAL_TABLE_WITNESS_UPDATES, globalTableWitnessUpdates, globalTableWitnessUpdates.size(), SharedSerde.GlobalTableWitnessGroupUpdateListSerializer.INSTANCE);
        }
        if (onDemandThroughput != null) {
            serializer.writeStruct($SCHEMA_ON_DEMAND_THROUGHPUT, onDemandThroughput);
        }
        if (warmThroughput != null) {
            serializer.writeStruct($SCHEMA_WARM_THROUGHPUT, warmThroughput);
        }
        if (globalTableSettingsReplicationMode != null) {
            serializer.writeString($SCHEMA_GLOBAL_TABLE_SETTINGS_REPLICATION_MODE, globalTableSettingsReplicationMode.getValue());
        }
        if (vectorIndexUpdates != null) {
            serializer.writeList($SCHEMA_VECTOR_INDEX_UPDATES, vectorIndexUpdates, vectorIndexUpdates.size(), SharedSerde.VectorIndexUpdateListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_DEFINITIONS, member, attributeDefinitions);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_BILLING_MODE, member, billingMode);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_THROUGHPUT, member, provisionedThroughput);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_SECONDARY_INDEX_UPDATES, member, globalSecondaryIndexUpdates);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_STREAM_SPECIFICATION, member, streamSpecification);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_SSE_SPECIFICATION, member, sseSpecification);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_UPDATES, member, replicaUpdates);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_CLASS, member, tableClass);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELETION_PROTECTION_ENABLED, member, deletionProtectionEnabled);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_MULTI_REGION_CONSISTENCY, member, multiRegionConsistency);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_WITNESS_UPDATES, member, globalTableWitnessUpdates);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_ON_DEMAND_THROUGHPUT, member, onDemandThroughput);
            case 13 -> (T) SchemaUtils.validateSameMember($SCHEMA_WARM_THROUGHPUT, member, warmThroughput);
            case 14 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_SETTINGS_REPLICATION_MODE, member, globalTableSettingsReplicationMode);
            case 15 -> (T) SchemaUtils.validateSameMember($SCHEMA_VECTOR_INDEX_UPDATES, member, vectorIndexUpdates);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link UpdateTableInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.attributeDefinitions(this.attributeDefinitions);
        builder.tableName(this.tableName);
        builder.billingMode(this.billingMode);
        builder.provisionedThroughput(this.provisionedThroughput);
        builder.globalSecondaryIndexUpdates(this.globalSecondaryIndexUpdates);
        builder.streamSpecification(this.streamSpecification);
        builder.sseSpecification(this.sseSpecification);
        builder.replicaUpdates(this.replicaUpdates);
        builder.tableClass(this.tableClass);
        builder.deletionProtectionEnabled(this.deletionProtectionEnabled);
        builder.multiRegionConsistency(this.multiRegionConsistency);
        builder.globalTableWitnessUpdates(this.globalTableWitnessUpdates);
        builder.onDemandThroughput(this.onDemandThroughput);
        builder.warmThroughput(this.warmThroughput);
        builder.globalTableSettingsReplicationMode(this.globalTableSettingsReplicationMode);
        builder.vectorIndexUpdates(this.vectorIndexUpdates);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link UpdateTableInput}.
     */
    public static final class Builder implements ShapeBuilder<UpdateTableInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private List<AttributeDefinition> attributeDefinitions;
        private String tableName;
        private BillingMode billingMode;
        private ProvisionedThroughput provisionedThroughput;
        private List<GlobalSecondaryIndexUpdate> globalSecondaryIndexUpdates;
        private StreamSpecification streamSpecification;
        private SSESpecification sseSpecification;
        private List<ReplicationGroupUpdate> replicaUpdates;
        private TableClass tableClass;
        private Boolean deletionProtectionEnabled;
        private MultiRegionConsistency multiRegionConsistency;
        private List<GlobalTableWitnessGroupUpdate> globalTableWitnessUpdates;
        private OnDemandThroughput onDemandThroughput;
        private WarmThroughput warmThroughput;
        private GlobalTableSettingsReplicationMode globalTableSettingsReplicationMode;
        private List<VectorIndexUpdate> vectorIndexUpdates;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * An array of attributes that describe the key schema for the table and indexes. If you are adding a new global
         * secondary index to the table, <code>AttributeDefinitions</code> must include the key element(s) of the new index.
         *
         * @return this builder.
         */
        public Builder attributeDefinitions(List<AttributeDefinition> attributeDefinitions) {
            this.attributeDefinitions = attributeDefinitions;
            return this;
        }

        /**
         * The name of the table to be updated. You can also provide the Amazon Resource Name (ARN) of the table in this
         * parameter.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder tableName(String tableName) {
            this.tableName = Objects.requireNonNull(tableName, "tableName cannot be null");
            tracker.setMember($SCHEMA_TABLE_NAME);
            return this;
        }

        /**
         * Controls how you are charged for read and write throughput and how you manage capacity. When switching from
         * pay-per-request to provisioned capacity, initial provisioned capacity values must be set. The initial provisioned
         * capacity values are estimated based on the consumed read and write capacity of your table and global secondary
         * indexes over the past 30 minutes.
         *
         * <ul>
         *   <li>
         *     <code>PAY_PER_REQUEST</code> - We recommend using <code>PAY_PER_REQUEST</code> for most DynamoDB
         *     workloads. <code>PAY_PER_REQUEST</code> sets the billing mode to <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/on-demand-capacity-mode.html">On-demand capacity mode</a>.
         *   </li>
         *   <li>
         *     <code>PROVISIONED</code> - We recommend using <code>PROVISIONED</code> for steady workloads with
         *     predictable growth where capacity requirements can be reliably forecasted. <code>PROVISIONED</code> sets
         *     the billing mode to <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/provisioned-capacity-mode.html">Provisioned capacity mode</a>.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder billingMode(BillingMode billingMode) {
            this.billingMode = billingMode;
            return this;
        }

        /**
         * The new provisioned throughput settings for the specified table or index.
         *
         * @return this builder.
         */
        public Builder provisionedThroughput(ProvisionedThroughput provisionedThroughput) {
            this.provisionedThroughput = provisionedThroughput;
            return this;
        }

        /**
         * An array of one or more global secondary indexes for the table. For each index in the array, you can request one
         * action:
         *
         * <ul>
         *   <li>
         *     <code>Create</code> - add a new global secondary index to the table.
         *   </li>
         *   <li>
         *     <code>Update</code> - modify the provisioned throughput settings of an existing global secondary index.
         *   </li>
         *   <li>
         *     <code>Delete</code> - remove a global secondary index from the table.
         *   </li>
         * </ul>
         *
         * <p>You can create or delete only one global secondary index per <code>UpdateTable</code> operation.
         *
         * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GSI.OnlineOps.html">Managing Global Secondary Indexes</a> in the <i>Amazon DynamoDB Developer Guide</i>
         * .
         *
         * @return this builder.
         */
        public Builder globalSecondaryIndexUpdates(List<GlobalSecondaryIndexUpdate> globalSecondaryIndexUpdates) {
            this.globalSecondaryIndexUpdates = globalSecondaryIndexUpdates;
            return this;
        }

        /**
         * Represents the DynamoDB Streams configuration for the table.
         *
         * <p>You receive a <code>ValidationException</code> if you try to enable a stream on a table that already has a
         * stream, or if you try to disable a stream on a table that doesn't have a stream.
         *
         * @return this builder.
         */
        public Builder streamSpecification(StreamSpecification streamSpecification) {
            this.streamSpecification = streamSpecification;
            return this;
        }

        /**
         * The new server-side encryption settings for the specified table.
         *
         * @return this builder.
         */
        public Builder sseSpecification(SSESpecification sseSpecification) {
            this.sseSpecification = sseSpecification;
            return this;
        }

        /**
         * A list of replica update actions (create, delete, or update) for the table.
         *
         * @return this builder.
         */
        public Builder replicaUpdates(List<ReplicationGroupUpdate> replicaUpdates) {
            this.replicaUpdates = replicaUpdates;
            return this;
        }

        /**
         * The table class of the table to be updated. Valid values are <code>STANDARD</code> and
         * <code>STANDARD_INFREQUENT_ACCESS</code>.
         *
         * @return this builder.
         */
        public Builder tableClass(TableClass tableClass) {
            this.tableClass = tableClass;
            return this;
        }

        /**
         * Indicates whether deletion protection is to be enabled (true) or disabled (false) on the table.
         *
         * @return this builder.
         */
        public Builder deletionProtectionEnabled(Boolean deletionProtectionEnabled) {
            this.deletionProtectionEnabled = deletionProtectionEnabled;
            return this;
        }

        /**
         * Specifies the consistency mode for a new global table. This parameter is only valid when you create a global
         * table by specifying one or more <a href="https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_ReplicationGroupUpdate.html#DDB-Type-ReplicationGroupUpdate-Create">Create</a> actions in the <a href="https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_UpdateTable.html#DDB-UpdateTable-request-ReplicaUpdates">ReplicaUpdates</a> action list.
         *
         * <p>You can specify one of the following consistency modes:
         *
         * <ul>
         *   <li>
         *     <code>EVENTUAL</code>: Configures a new global table for multi-Region eventual consistency (MREC). This
         *     is the default consistency mode for global tables.
         *   </li>
         *   <li>
         *     <code>STRONG</code>: Configures a new global table for multi-Region strong consistency (MRSC).
         *   </li>
         * </ul>
         *
         * <p>If you don't specify this field, the global table consistency mode defaults to <code>EVENTUAL</code>. For more
         * information about global tables consistency modes, see <a href="https://docs.aws.amazon.com/V2globaltables_HowItWorks.html#V2globaltables_HowItWorks.consistency-modes"> Consistency modes</a> in DynamoDB developer guide.
         *
         * @return this builder.
         */
        public Builder multiRegionConsistency(MultiRegionConsistency multiRegionConsistency) {
            this.multiRegionConsistency = multiRegionConsistency;
            return this;
        }

        /**
         * A list of witness updates for a MRSC global table. A witness provides a cost-effective alternative to a full
         * replica in a MRSC global table by maintaining replicated change data written to global table replicas. You cannot
         * perform read or write operations on a witness. For each witness, you can request one action:
         *
         * <ul>
         *   <li>
         *     <code>Create</code> - add a new witness to the global table.
         *   </li>
         *   <li>
         *     <code>Delete</code> - remove a witness from the global table.
         *   </li>
         * </ul>
         *
         * <p>You can create or delete only one witness per <code>UpdateTable</code> operation.
         *
         * <p>For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/V2globaltables_HowItWorks.html#V2globaltables_HowItWorks.consistency-modes">Multi-Region strong consistency (MRSC)</a> in the Amazon DynamoDB Developer Guide
         *
         * @return this builder.
         */
        public Builder globalTableWitnessUpdates(List<GlobalTableWitnessGroupUpdate> globalTableWitnessUpdates) {
            this.globalTableWitnessUpdates = globalTableWitnessUpdates;
            return this;
        }

        /**
         * Updates the maximum number of read and write units for the specified table in on-demand capacity mode. If you use
         * this parameter, you must specify <code>MaxReadRequestUnits</code>, <code>MaxWriteRequestUnits</code>, or both.
         *
         * @return this builder.
         */
        public Builder onDemandThroughput(OnDemandThroughput onDemandThroughput) {
            this.onDemandThroughput = onDemandThroughput;
            return this;
        }

        /**
         * Represents the warm throughput (in read units per second and write units per second) for updating a table.
         *
         * @return this builder.
         */
        public Builder warmThroughput(WarmThroughput warmThroughput) {
            this.warmThroughput = warmThroughput;
            return this;
        }

        /**
         * Controls the settings replication mode for a global table replica. This attribute can be defined using
         * UpdateTable operation only on a regional table with values:
         *
         * <ul>
         *   <li>
         *     <code>ENABLED</code>: Defines settings replication on a regional table to be used as a source table for
         *     creating Multi-Account Global Table.
         *   </li>
         *   <li>
         *     <code>DISABLED</code>: Remove settings replication on a regional table. Settings replication needs to be
         *     defined to ENABLED again in order to create a Multi-Account Global Table using this table.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder globalTableSettingsReplicationMode(GlobalTableSettingsReplicationMode globalTableSettingsReplicationMode) {
            this.globalTableSettingsReplicationMode = globalTableSettingsReplicationMode;
            return this;
        }

        /**
         * A list of vector indexes to be added to or removed from the table. You can add or remove one vector index for
         * each <code>UpdateTable</code> operation.
         *
         * <p>To add a vector index, specify <code>IndexName</code>, <code>VectorAttribute</code>, <code>Dimensions</code>, <code>
         * DistanceFunction</code>, and <code>Projection</code>. To remove a vector index, specify only the <code>IndexName</code>
         * .
         *
         * @return this builder.
         */
        public Builder vectorIndexUpdates(List<VectorIndexUpdate> vectorIndexUpdates) {
            this.vectorIndexUpdates = vectorIndexUpdates;
            return this;
        }

        @Override
        public UpdateTableInput build() {
            tracker.validate();
            return new UpdateTableInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 1 -> attributeDefinitions((List<AttributeDefinition>) SchemaUtils.validateSameMember($SCHEMA_ATTRIBUTE_DEFINITIONS, member, value));
                case 2 -> billingMode((BillingMode) SchemaUtils.validateSameMember($SCHEMA_BILLING_MODE, member, value));
                case 3 -> provisionedThroughput((ProvisionedThroughput) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_THROUGHPUT, member, value));
                case 4 -> globalSecondaryIndexUpdates((List<GlobalSecondaryIndexUpdate>) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_SECONDARY_INDEX_UPDATES, member, value));
                case 5 -> streamSpecification((StreamSpecification) SchemaUtils.validateSameMember($SCHEMA_STREAM_SPECIFICATION, member, value));
                case 6 -> sseSpecification((SSESpecification) SchemaUtils.validateSameMember($SCHEMA_SSE_SPECIFICATION, member, value));
                case 7 -> replicaUpdates((List<ReplicationGroupUpdate>) SchemaUtils.validateSameMember($SCHEMA_REPLICA_UPDATES, member, value));
                case 8 -> tableClass((TableClass) SchemaUtils.validateSameMember($SCHEMA_TABLE_CLASS, member, value));
                case 9 -> deletionProtectionEnabled((Boolean) SchemaUtils.validateSameMember($SCHEMA_DELETION_PROTECTION_ENABLED, member, value));
                case 10 -> multiRegionConsistency((MultiRegionConsistency) SchemaUtils.validateSameMember($SCHEMA_MULTI_REGION_CONSISTENCY, member, value));
                case 11 -> globalTableWitnessUpdates((List<GlobalTableWitnessGroupUpdate>) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_WITNESS_UPDATES, member, value));
                case 12 -> onDemandThroughput((OnDemandThroughput) SchemaUtils.validateSameMember($SCHEMA_ON_DEMAND_THROUGHPUT, member, value));
                case 13 -> warmThroughput((WarmThroughput) SchemaUtils.validateSameMember($SCHEMA_WARM_THROUGHPUT, member, value));
                case 14 -> globalTableSettingsReplicationMode((GlobalTableSettingsReplicationMode) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_SETTINGS_REPLICATION_MODE, member, value));
                case 15 -> vectorIndexUpdates((List<VectorIndexUpdate>) SchemaUtils.validateSameMember($SCHEMA_VECTOR_INDEX_UPDATES, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<UpdateTableInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_TABLE_NAME)) {
                tableName("");
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
                    case 0 -> builder.tableName(de.readString(member));
                    case 1 -> builder.attributeDefinitions(SharedSerde.deserializeAttributeDefinitions(member, de));
                    case 2 -> builder.billingMode(BillingMode.builder().deserializeMember(de, member).build());
                    case 3 -> builder.provisionedThroughput(ProvisionedThroughput.builder().deserializeMember(de, member).build());
                    case 4 -> builder.globalSecondaryIndexUpdates(SharedSerde.deserializeGlobalSecondaryIndexUpdateList(member, de));
                    case 5 -> builder.streamSpecification(StreamSpecification.builder().deserializeMember(de, member).build());
                    case 6 -> builder.sseSpecification(SSESpecification.builder().deserializeMember(de, member).build());
                    case 7 -> builder.replicaUpdates(SharedSerde.deserializeReplicationGroupUpdateList(member, de));
                    case 8 -> builder.tableClass(TableClass.builder().deserializeMember(de, member).build());
                    case 9 -> builder.deletionProtectionEnabled(de.readBoolean(member));
                    case 10 -> builder.multiRegionConsistency(MultiRegionConsistency.builder().deserializeMember(de, member).build());
                    case 11 -> builder.globalTableWitnessUpdates(SharedSerde.deserializeGlobalTableWitnessGroupUpdateList(member, de));
                    case 12 -> builder.onDemandThroughput(OnDemandThroughput.builder().deserializeMember(de, member).build());
                    case 13 -> builder.warmThroughput(WarmThroughput.builder().deserializeMember(de, member).build());
                    case 14 -> builder.globalTableSettingsReplicationMode(GlobalTableSettingsReplicationMode.builder().deserializeMember(de, member).build());
                    case 15 -> builder.vectorIndexUpdates(SharedSerde.deserializeVectorIndexUpdateList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
