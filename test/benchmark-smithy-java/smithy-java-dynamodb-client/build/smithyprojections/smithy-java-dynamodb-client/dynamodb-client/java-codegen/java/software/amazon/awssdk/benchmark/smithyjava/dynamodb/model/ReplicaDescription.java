package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

import java.time.Instant;
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

/**
 * Contains the details of the replica.
 */
@SmithyGenerated
public final class ReplicaDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.REPLICA_DESCRIPTION;
    private static final Schema $SCHEMA_REGION_NAME = $SCHEMA.member("RegionName");
    private static final Schema $SCHEMA_REPLICA_STATUS = $SCHEMA.member("ReplicaStatus");
    private static final Schema $SCHEMA_REPLICA_ARN = $SCHEMA.member("ReplicaArn");
    private static final Schema $SCHEMA_REPLICA_STATUS_DESCRIPTION = $SCHEMA.member("ReplicaStatusDescription");
    private static final Schema $SCHEMA_REPLICA_STATUS_PERCENT_PROGRESS = $SCHEMA.member("ReplicaStatusPercentProgress");
    private static final Schema $SCHEMA_KMS_MASTER_KEY_ID = $SCHEMA.member("KMSMasterKeyId");
    private static final Schema $SCHEMA_PROVISIONED_THROUGHPUT_OVERRIDE = $SCHEMA.member("ProvisionedThroughputOverride");
    private static final Schema $SCHEMA_ON_DEMAND_THROUGHPUT_OVERRIDE = $SCHEMA.member("OnDemandThroughputOverride");
    private static final Schema $SCHEMA_WARM_THROUGHPUT = $SCHEMA.member("WarmThroughput");
    private static final Schema $SCHEMA_GLOBAL_SECONDARY_INDEXES = $SCHEMA.member("GlobalSecondaryIndexes");
    private static final Schema $SCHEMA_REPLICA_INACCESSIBLE_DATE_TIME = $SCHEMA.member("ReplicaInaccessibleDateTime");
    private static final Schema $SCHEMA_REPLICA_TABLE_CLASS_SUMMARY = $SCHEMA.member("ReplicaTableClassSummary");
    private static final Schema $SCHEMA_GLOBAL_TABLE_SETTINGS_REPLICATION_MODE = $SCHEMA.member("GlobalTableSettingsReplicationMode");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String regionName;
    private final transient ReplicaStatus replicaStatus;
    private final transient String replicaArn;
    private final transient String replicaStatusDescription;
    private final transient String replicaStatusPercentProgress;
    private final transient String kmsMasterKeyId;
    private final transient ProvisionedThroughputOverride provisionedThroughputOverride;
    private final transient OnDemandThroughputOverride onDemandThroughputOverride;
    private final transient TableWarmThroughputDescription warmThroughput;
    private final transient List<ReplicaGlobalSecondaryIndexDescription> globalSecondaryIndexes;
    private final transient Instant replicaInaccessibleDateTime;
    private final transient TableClassSummary replicaTableClassSummary;
    private final transient GlobalTableSettingsReplicationMode globalTableSettingsReplicationMode;

    private ReplicaDescription(Builder builder) {
        this.regionName = builder.regionName;
        this.replicaStatus = builder.replicaStatus;
        this.replicaArn = builder.replicaArn;
        this.replicaStatusDescription = builder.replicaStatusDescription;
        this.replicaStatusPercentProgress = builder.replicaStatusPercentProgress;
        this.kmsMasterKeyId = builder.kmsMasterKeyId;
        this.provisionedThroughputOverride = builder.provisionedThroughputOverride;
        this.onDemandThroughputOverride = builder.onDemandThroughputOverride;
        this.warmThroughput = builder.warmThroughput;
        this.globalSecondaryIndexes = builder.globalSecondaryIndexes == null ? null : Collections.unmodifiableList(builder.globalSecondaryIndexes);
        this.replicaInaccessibleDateTime = builder.replicaInaccessibleDateTime;
        this.replicaTableClassSummary = builder.replicaTableClassSummary;
        this.globalTableSettingsReplicationMode = builder.globalTableSettingsReplicationMode;
    }

    /**
     * The name of the Region.
     */
    public String getRegionName() {
        return regionName;
    }

    /**
     * The current state of the replica:
     *
     * <ul>
     *   <li>
     *     <code>CREATING</code> - The replica is being created.
     *   </li>
     *   <li>
     *     <code>UPDATING</code> - The replica is being updated.
     *   </li>
     *   <li>
     *     <code>DELETING</code> - The replica is being deleted.
     *   </li>
     *   <li>
     *     <code>ACTIVE</code> - The replica is ready for use.
     *   </li>
     *   <li>
     *     <code>REGION_DISABLED</code> - The replica is inaccessible because the Amazon Web Services Region has
     *     been disabled.If the Amazon Web Services Region remains inaccessible for more than 20 hours, DynamoDB
     *     will remove this replica from the replication group. The replica will not be deleted and replication will
     *     stop from and to this region.
     *   </li>
     *   <li>
     *     <code>INACCESSIBLE_ENCRYPTION_CREDENTIALS </code> - The KMS key used to encrypt the table is
     *     inaccessible.If the KMS key remains inaccessible for more than 20 hours, DynamoDB will remove this
     *     replica from the replication group. The replica will not be deleted and replication will stop from and to
     *     this region.
     *   </li>
     * </ul>
     */
    public ReplicaStatus getReplicaStatus() {
        return replicaStatus;
    }

    /**
     * The Amazon Resource Name (ARN) of the global table replica.
     */
    public String getReplicaArn() {
        return replicaArn;
    }

    /**
     * Detailed information about the replica status.
     */
    public String getReplicaStatusDescription() {
        return replicaStatusDescription;
    }

    /**
     * Specifies the progress of a Create, Update, or Delete action on the replica as a percentage.
     */
    public String getReplicaStatusPercentProgress() {
        return replicaStatusPercentProgress;
    }

    /**
     * The KMS key of the replica that will be used for KMS encryption.
     */
    public String getKmsMasterKeyId() {
        return kmsMasterKeyId;
    }

    /**
     * Replica-specific provisioned throughput. If not described, uses the source table's provisioned throughput
     * settings.
     */
    public ProvisionedThroughputOverride getProvisionedThroughputOverride() {
        return provisionedThroughputOverride;
    }

    /**
     * Overrides the maximum on-demand throughput settings for the specified replica table.
     */
    public OnDemandThroughputOverride getOnDemandThroughputOverride() {
        return onDemandThroughputOverride;
    }

    /**
     * Represents the warm throughput value for this replica.
     */
    public TableWarmThroughputDescription getWarmThroughput() {
        return warmThroughput;
    }

    /**
     * Replica-specific global secondary index settings.
     */
    public List<ReplicaGlobalSecondaryIndexDescription> getGlobalSecondaryIndexes() {
        if (globalSecondaryIndexes == null) {
            return Collections.emptyList();
        }
        return globalSecondaryIndexes;
    }

    public boolean hasGlobalSecondaryIndexes() {
        return globalSecondaryIndexes != null;
    }

    /**
     * The time at which the replica was first detected as inaccessible. To determine cause of inaccessibility check the
     * <code>ReplicaStatus</code> property.
     */
    public Instant getReplicaInaccessibleDateTime() {
        return replicaInaccessibleDateTime;
    }

    public TableClassSummary getReplicaTableClassSummary() {
        return replicaTableClassSummary;
    }

    /**
     * Indicates one of the settings synchronization modes for the global table replica:
     *
     * <ul>
     *   <li>
     *     <code>ENABLED</code>: Indicates that the settings synchronization mode for the global table replica is
     *     enabled.
     *   </li>
     *   <li>
     *     <code>DISABLED</code>: Indicates that the settings synchronization mode for the global table replica is
     *     disabled.
     *   </li>
     *   <li>
     *     <code>ENABLED_WITH_OVERRIDES</code>: This mode is set by default for a same account global table.
     *     Indicates that certain global table settings can be overridden.
     *   </li>
     * </ul>
     */
    public GlobalTableSettingsReplicationMode getGlobalTableSettingsReplicationMode() {
        return globalTableSettingsReplicationMode;
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
        ReplicaDescription that = (ReplicaDescription) other;
        return Objects.equals(this.regionName, that.regionName)
               && Objects.equals(this.replicaArn, that.replicaArn)
               && Objects.equals(this.replicaStatusDescription, that.replicaStatusDescription)
               && Objects.equals(this.replicaStatusPercentProgress, that.replicaStatusPercentProgress)
               && Objects.equals(this.kmsMasterKeyId, that.kmsMasterKeyId)
               && Objects.equals(this.replicaStatus, that.replicaStatus)
               && Objects.equals(this.globalTableSettingsReplicationMode, that.globalTableSettingsReplicationMode)
               && Objects.equals(this.replicaInaccessibleDateTime, that.replicaInaccessibleDateTime)
               && Objects.equals(this.provisionedThroughputOverride, that.provisionedThroughputOverride)
               && Objects.equals(this.onDemandThroughputOverride, that.onDemandThroughputOverride)
               && Objects.equals(this.warmThroughput, that.warmThroughput)
               && Objects.equals(this.replicaTableClassSummary, that.replicaTableClassSummary)
               && Objects.equals(this.globalSecondaryIndexes, that.globalSecondaryIndexes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(regionName);
        $hc = 31 * $hc + Objects.hashCode(replicaStatus);
        $hc = 31 * $hc + Objects.hashCode(replicaArn);
        $hc = 31 * $hc + Objects.hashCode(replicaStatusDescription);
        $hc = 31 * $hc + Objects.hashCode(replicaStatusPercentProgress);
        $hc = 31 * $hc + Objects.hashCode(kmsMasterKeyId);
        $hc = 31 * $hc + Objects.hashCode(provisionedThroughputOverride);
        $hc = 31 * $hc + Objects.hashCode(onDemandThroughputOverride);
        $hc = 31 * $hc + Objects.hashCode(warmThroughput);
        $hc = 31 * $hc + Objects.hashCode(globalSecondaryIndexes);
        $hc = 31 * $hc + Objects.hashCode(replicaInaccessibleDateTime);
        $hc = 31 * $hc + Objects.hashCode(replicaTableClassSummary);
        $hc = 31 * $hc + Objects.hashCode(globalTableSettingsReplicationMode);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (regionName != null) {
            serializer.writeString($SCHEMA_REGION_NAME, regionName);
        }
        if (replicaStatus != null) {
            serializer.writeString($SCHEMA_REPLICA_STATUS, replicaStatus.getValue());
        }
        if (replicaArn != null) {
            serializer.writeString($SCHEMA_REPLICA_ARN, replicaArn);
        }
        if (replicaStatusDescription != null) {
            serializer.writeString($SCHEMA_REPLICA_STATUS_DESCRIPTION, replicaStatusDescription);
        }
        if (replicaStatusPercentProgress != null) {
            serializer.writeString($SCHEMA_REPLICA_STATUS_PERCENT_PROGRESS, replicaStatusPercentProgress);
        }
        if (kmsMasterKeyId != null) {
            serializer.writeString($SCHEMA_KMS_MASTER_KEY_ID, kmsMasterKeyId);
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
        if (globalSecondaryIndexes != null) {
            serializer.writeList($SCHEMA_GLOBAL_SECONDARY_INDEXES, globalSecondaryIndexes, globalSecondaryIndexes.size(), SharedSerde.ReplicaGlobalSecondaryIndexDescriptionListSerializer.INSTANCE);
        }
        if (replicaInaccessibleDateTime != null) {
            serializer.writeTimestamp($SCHEMA_REPLICA_INACCESSIBLE_DATE_TIME, replicaInaccessibleDateTime);
        }
        if (replicaTableClassSummary != null) {
            serializer.writeStruct($SCHEMA_REPLICA_TABLE_CLASS_SUMMARY, replicaTableClassSummary);
        }
        if (globalTableSettingsReplicationMode != null) {
            serializer.writeString($SCHEMA_GLOBAL_TABLE_SETTINGS_REPLICATION_MODE, globalTableSettingsReplicationMode.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_REGION_NAME, member, regionName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_STATUS, member, replicaStatus);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_ARN, member, replicaArn);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_STATUS_DESCRIPTION, member, replicaStatusDescription);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_STATUS_PERCENT_PROGRESS, member, replicaStatusPercentProgress);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_KMS_MASTER_KEY_ID, member, kmsMasterKeyId);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_THROUGHPUT_OVERRIDE, member, provisionedThroughputOverride);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_ON_DEMAND_THROUGHPUT_OVERRIDE, member, onDemandThroughputOverride);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_WARM_THROUGHPUT, member, warmThroughput);
            case 9 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_SECONDARY_INDEXES, member, globalSecondaryIndexes);
            case 10 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_INACCESSIBLE_DATE_TIME, member, replicaInaccessibleDateTime);
            case 11 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_TABLE_CLASS_SUMMARY, member, replicaTableClassSummary);
            case 12 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_SETTINGS_REPLICATION_MODE, member, globalTableSettingsReplicationMode);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ReplicaDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.regionName(this.regionName);
        builder.replicaStatus(this.replicaStatus);
        builder.replicaArn(this.replicaArn);
        builder.replicaStatusDescription(this.replicaStatusDescription);
        builder.replicaStatusPercentProgress(this.replicaStatusPercentProgress);
        builder.kmsMasterKeyId(this.kmsMasterKeyId);
        builder.provisionedThroughputOverride(this.provisionedThroughputOverride);
        builder.onDemandThroughputOverride(this.onDemandThroughputOverride);
        builder.warmThroughput(this.warmThroughput);
        builder.globalSecondaryIndexes(this.globalSecondaryIndexes);
        builder.replicaInaccessibleDateTime(this.replicaInaccessibleDateTime);
        builder.replicaTableClassSummary(this.replicaTableClassSummary);
        builder.globalTableSettingsReplicationMode(this.globalTableSettingsReplicationMode);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ReplicaDescription}.
     */
    public static final class Builder implements ShapeBuilder<ReplicaDescription> {
        private String regionName;
        private ReplicaStatus replicaStatus;
        private String replicaArn;
        private String replicaStatusDescription;
        private String replicaStatusPercentProgress;
        private String kmsMasterKeyId;
        private ProvisionedThroughputOverride provisionedThroughputOverride;
        private OnDemandThroughputOverride onDemandThroughputOverride;
        private TableWarmThroughputDescription warmThroughput;
        private List<ReplicaGlobalSecondaryIndexDescription> globalSecondaryIndexes;
        private Instant replicaInaccessibleDateTime;
        private TableClassSummary replicaTableClassSummary;
        private GlobalTableSettingsReplicationMode globalTableSettingsReplicationMode;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the Region.
         *
         * @return this builder.
         */
        public Builder regionName(String regionName) {
            this.regionName = regionName;
            return this;
        }

        /**
         * The current state of the replica:
         *
         * <ul>
         *   <li>
         *     <code>CREATING</code> - The replica is being created.
         *   </li>
         *   <li>
         *     <code>UPDATING</code> - The replica is being updated.
         *   </li>
         *   <li>
         *     <code>DELETING</code> - The replica is being deleted.
         *   </li>
         *   <li>
         *     <code>ACTIVE</code> - The replica is ready for use.
         *   </li>
         *   <li>
         *     <code>REGION_DISABLED</code> - The replica is inaccessible because the Amazon Web Services Region has
         *     been disabled.If the Amazon Web Services Region remains inaccessible for more than 20 hours, DynamoDB
         *     will remove this replica from the replication group. The replica will not be deleted and replication will
         *     stop from and to this region.
         *   </li>
         *   <li>
         *     <code>INACCESSIBLE_ENCRYPTION_CREDENTIALS </code> - The KMS key used to encrypt the table is
         *     inaccessible.If the KMS key remains inaccessible for more than 20 hours, DynamoDB will remove this
         *     replica from the replication group. The replica will not be deleted and replication will stop from and to
         *     this region.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder replicaStatus(ReplicaStatus replicaStatus) {
            this.replicaStatus = replicaStatus;
            return this;
        }

        /**
         * The Amazon Resource Name (ARN) of the global table replica.
         *
         * @return this builder.
         */
        public Builder replicaArn(String replicaArn) {
            this.replicaArn = replicaArn;
            return this;
        }

        /**
         * Detailed information about the replica status.
         *
         * @return this builder.
         */
        public Builder replicaStatusDescription(String replicaStatusDescription) {
            this.replicaStatusDescription = replicaStatusDescription;
            return this;
        }

        /**
         * Specifies the progress of a Create, Update, or Delete action on the replica as a percentage.
         *
         * @return this builder.
         */
        public Builder replicaStatusPercentProgress(String replicaStatusPercentProgress) {
            this.replicaStatusPercentProgress = replicaStatusPercentProgress;
            return this;
        }

        /**
         * The KMS key of the replica that will be used for KMS encryption.
         *
         * @return this builder.
         */
        public Builder kmsMasterKeyId(String kmsMasterKeyId) {
            this.kmsMasterKeyId = kmsMasterKeyId;
            return this;
        }

        /**
         * Replica-specific provisioned throughput. If not described, uses the source table's provisioned throughput
         * settings.
         *
         * @return this builder.
         */
        public Builder provisionedThroughputOverride(ProvisionedThroughputOverride provisionedThroughputOverride) {
            this.provisionedThroughputOverride = provisionedThroughputOverride;
            return this;
        }

        /**
         * Overrides the maximum on-demand throughput settings for the specified replica table.
         *
         * @return this builder.
         */
        public Builder onDemandThroughputOverride(OnDemandThroughputOverride onDemandThroughputOverride) {
            this.onDemandThroughputOverride = onDemandThroughputOverride;
            return this;
        }

        /**
         * Represents the warm throughput value for this replica.
         *
         * @return this builder.
         */
        public Builder warmThroughput(TableWarmThroughputDescription warmThroughput) {
            this.warmThroughput = warmThroughput;
            return this;
        }

        /**
         * Replica-specific global secondary index settings.
         *
         * @return this builder.
         */
        public Builder globalSecondaryIndexes(List<ReplicaGlobalSecondaryIndexDescription> globalSecondaryIndexes) {
            this.globalSecondaryIndexes = globalSecondaryIndexes;
            return this;
        }

        /**
         * The time at which the replica was first detected as inaccessible. To determine cause of inaccessibility check the
         * <code>ReplicaStatus</code> property.
         *
         * @return this builder.
         */
        public Builder replicaInaccessibleDateTime(Instant replicaInaccessibleDateTime) {
            this.replicaInaccessibleDateTime = replicaInaccessibleDateTime;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder replicaTableClassSummary(TableClassSummary replicaTableClassSummary) {
            this.replicaTableClassSummary = replicaTableClassSummary;
            return this;
        }

        /**
         * Indicates one of the settings synchronization modes for the global table replica:
         *
         * <ul>
         *   <li>
         *     <code>ENABLED</code>: Indicates that the settings synchronization mode for the global table replica is
         *     enabled.
         *   </li>
         *   <li>
         *     <code>DISABLED</code>: Indicates that the settings synchronization mode for the global table replica is
         *     disabled.
         *   </li>
         *   <li>
         *     <code>ENABLED_WITH_OVERRIDES</code>: This mode is set by default for a same account global table.
         *     Indicates that certain global table settings can be overridden.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder globalTableSettingsReplicationMode(GlobalTableSettingsReplicationMode globalTableSettingsReplicationMode) {
            this.globalTableSettingsReplicationMode = globalTableSettingsReplicationMode;
            return this;
        }

        @Override
        public ReplicaDescription build() {
            return new ReplicaDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> regionName((String) SchemaUtils.validateSameMember($SCHEMA_REGION_NAME, member, value));
                case 1 -> replicaStatus((ReplicaStatus) SchemaUtils.validateSameMember($SCHEMA_REPLICA_STATUS, member, value));
                case 2 -> replicaArn((String) SchemaUtils.validateSameMember($SCHEMA_REPLICA_ARN, member, value));
                case 3 -> replicaStatusDescription((String) SchemaUtils.validateSameMember($SCHEMA_REPLICA_STATUS_DESCRIPTION, member, value));
                case 4 -> replicaStatusPercentProgress((String) SchemaUtils.validateSameMember($SCHEMA_REPLICA_STATUS_PERCENT_PROGRESS, member, value));
                case 5 -> kmsMasterKeyId((String) SchemaUtils.validateSameMember($SCHEMA_KMS_MASTER_KEY_ID, member, value));
                case 6 -> provisionedThroughputOverride((ProvisionedThroughputOverride) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_THROUGHPUT_OVERRIDE, member, value));
                case 7 -> onDemandThroughputOverride((OnDemandThroughputOverride) SchemaUtils.validateSameMember($SCHEMA_ON_DEMAND_THROUGHPUT_OVERRIDE, member, value));
                case 8 -> warmThroughput((TableWarmThroughputDescription) SchemaUtils.validateSameMember($SCHEMA_WARM_THROUGHPUT, member, value));
                case 9 -> globalSecondaryIndexes((List<ReplicaGlobalSecondaryIndexDescription>) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_SECONDARY_INDEXES, member, value));
                case 10 -> replicaInaccessibleDateTime((Instant) SchemaUtils.validateSameMember($SCHEMA_REPLICA_INACCESSIBLE_DATE_TIME, member, value));
                case 11 -> replicaTableClassSummary((TableClassSummary) SchemaUtils.validateSameMember($SCHEMA_REPLICA_TABLE_CLASS_SUMMARY, member, value));
                case 12 -> globalTableSettingsReplicationMode((GlobalTableSettingsReplicationMode) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_SETTINGS_REPLICATION_MODE, member, value));
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
                    case 0 -> builder.regionName(de.readString(member));
                    case 1 -> builder.replicaStatus(ReplicaStatus.builder().deserializeMember(de, member).build());
                    case 2 -> builder.replicaArn(de.readString(member));
                    case 3 -> builder.replicaStatusDescription(de.readString(member));
                    case 4 -> builder.replicaStatusPercentProgress(de.readString(member));
                    case 5 -> builder.kmsMasterKeyId(de.readString(member));
                    case 6 -> builder.provisionedThroughputOverride(ProvisionedThroughputOverride.builder().deserializeMember(de, member).build());
                    case 7 -> builder.onDemandThroughputOverride(OnDemandThroughputOverride.builder().deserializeMember(de, member).build());
                    case 8 -> builder.warmThroughput(TableWarmThroughputDescription.builder().deserializeMember(de, member).build());
                    case 9 -> builder.globalSecondaryIndexes(SharedSerde.deserializeReplicaGlobalSecondaryIndexDescriptionList(member, de));
                    case 10 -> builder.replicaInaccessibleDateTime(de.readTimestamp(member));
                    case 11 -> builder.replicaTableClassSummary(TableClassSummary.builder().deserializeMember(de, member).build());
                    case 12 -> builder.globalTableSettingsReplicationMode(GlobalTableSettingsReplicationMode.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
