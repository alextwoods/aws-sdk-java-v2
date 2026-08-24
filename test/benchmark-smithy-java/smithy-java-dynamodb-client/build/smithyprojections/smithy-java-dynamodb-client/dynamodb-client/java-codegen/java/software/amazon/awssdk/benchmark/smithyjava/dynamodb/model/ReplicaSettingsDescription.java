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
 * Represents the properties of a replica.
 */
@SmithyGenerated
public final class ReplicaSettingsDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.REPLICA_SETTINGS_DESCRIPTION;
    private static final Schema $SCHEMA_REGION_NAME = $SCHEMA.member("RegionName");
    private static final Schema $SCHEMA_REPLICA_STATUS = $SCHEMA.member("ReplicaStatus");
    private static final Schema $SCHEMA_REPLICA_BILLING_MODE_SUMMARY = $SCHEMA.member("ReplicaBillingModeSummary");
    private static final Schema $SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_UNITS = $SCHEMA.member("ReplicaProvisionedReadCapacityUnits");
    private static final Schema $SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS = $SCHEMA.member("ReplicaProvisionedReadCapacityAutoScalingSettings");
    private static final Schema $SCHEMA_REPLICA_PROVISIONED_WRITE_CAPACITY_UNITS = $SCHEMA.member("ReplicaProvisionedWriteCapacityUnits");
    private static final Schema $SCHEMA_REPLICA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_SETTINGS = $SCHEMA.member("ReplicaProvisionedWriteCapacityAutoScalingSettings");
    private static final Schema $SCHEMA_REPLICA_GLOBAL_SECONDARY_INDEX_SETTINGS = $SCHEMA.member("ReplicaGlobalSecondaryIndexSettings");
    private static final Schema $SCHEMA_REPLICA_TABLE_CLASS_SUMMARY = $SCHEMA.member("ReplicaTableClassSummary");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String regionName;
    private final transient ReplicaStatus replicaStatus;
    private final transient BillingModeSummary replicaBillingModeSummary;
    private final transient Long replicaProvisionedReadCapacityUnits;
    private final transient AutoScalingSettingsDescription replicaProvisionedReadCapacityAutoScalingSettings;
    private final transient Long replicaProvisionedWriteCapacityUnits;
    private final transient AutoScalingSettingsDescription replicaProvisionedWriteCapacityAutoScalingSettings;
    private final transient List<ReplicaGlobalSecondaryIndexSettingsDescription> replicaGlobalSecondaryIndexSettings;
    private final transient TableClassSummary replicaTableClassSummary;

    private ReplicaSettingsDescription(Builder builder) {
        this.regionName = builder.regionName;
        this.replicaStatus = builder.replicaStatus;
        this.replicaBillingModeSummary = builder.replicaBillingModeSummary;
        this.replicaProvisionedReadCapacityUnits = builder.replicaProvisionedReadCapacityUnits;
        this.replicaProvisionedReadCapacityAutoScalingSettings = builder.replicaProvisionedReadCapacityAutoScalingSettings;
        this.replicaProvisionedWriteCapacityUnits = builder.replicaProvisionedWriteCapacityUnits;
        this.replicaProvisionedWriteCapacityAutoScalingSettings = builder.replicaProvisionedWriteCapacityAutoScalingSettings;
        this.replicaGlobalSecondaryIndexSettings = builder.replicaGlobalSecondaryIndexSettings == null ? null : Collections.unmodifiableList(builder.replicaGlobalSecondaryIndexSettings);
        this.replicaTableClassSummary = builder.replicaTableClassSummary;
    }

    /**
     * The Region name of the replica.
     */
    public String getRegionName() {
        return regionName;
    }

    /**
     * The current state of the Region:
     *
     * <ul>
     *   <li>
     *     <code>CREATING</code> - The Region is being created.
     *   </li>
     *   <li>
     *     <code>UPDATING</code> - The Region is being updated.
     *   </li>
     *   <li>
     *     <code>DELETING</code> - The Region is being deleted.
     *   </li>
     *   <li>
     *     <code>ACTIVE</code> - The Region is ready for use.
     *   </li>
     * </ul>
     */
    public ReplicaStatus getReplicaStatus() {
        return replicaStatus;
    }

    /**
     * The read/write capacity mode of the replica.
     */
    public BillingModeSummary getReplicaBillingModeSummary() {
        return replicaBillingModeSummary;
    }

    /**
     * The maximum number of strongly consistent reads consumed per second before DynamoDB returns a <code>
     * ThrottlingException</code>. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/WorkingWithTables.html#ProvisionedThroughput">Specifying Read and Write Requirements</a> in the <i>
     * Amazon DynamoDB Developer Guide</i>.
     */
    public Long getReplicaProvisionedReadCapacityUnits() {
        return replicaProvisionedReadCapacityUnits;
    }

    /**
     * Auto scaling settings for a global table replica's read capacity units.
     */
    public AutoScalingSettingsDescription getReplicaProvisionedReadCapacityAutoScalingSettings() {
        return replicaProvisionedReadCapacityAutoScalingSettings;
    }

    /**
     * The maximum number of writes consumed per second before DynamoDB returns a <code>ThrottlingException</code>. For
     * more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/WorkingWithTables.html#ProvisionedThroughput">Specifying Read and Write Requirements</a> in the <i>Amazon DynamoDB Developer Guide</i>
     * .
     */
    public Long getReplicaProvisionedWriteCapacityUnits() {
        return replicaProvisionedWriteCapacityUnits;
    }

    /**
     * Auto scaling settings for a global table replica's write capacity units.
     */
    public AutoScalingSettingsDescription getReplicaProvisionedWriteCapacityAutoScalingSettings() {
        return replicaProvisionedWriteCapacityAutoScalingSettings;
    }

    /**
     * Replica global secondary index settings for the global table.
     */
    public List<ReplicaGlobalSecondaryIndexSettingsDescription> getReplicaGlobalSecondaryIndexSettings() {
        if (replicaGlobalSecondaryIndexSettings == null) {
            return Collections.emptyList();
        }
        return replicaGlobalSecondaryIndexSettings;
    }

    public boolean hasReplicaGlobalSecondaryIndexSettings() {
        return replicaGlobalSecondaryIndexSettings != null;
    }

    public TableClassSummary getReplicaTableClassSummary() {
        return replicaTableClassSummary;
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
        ReplicaSettingsDescription that = (ReplicaSettingsDescription) other;
        return Objects.equals(this.replicaProvisionedReadCapacityUnits, that.replicaProvisionedReadCapacityUnits)
               && Objects.equals(this.replicaProvisionedWriteCapacityUnits, that.replicaProvisionedWriteCapacityUnits)
               && Objects.equals(this.regionName, that.regionName)
               && Objects.equals(this.replicaStatus, that.replicaStatus)
               && Objects.equals(this.replicaBillingModeSummary, that.replicaBillingModeSummary)
               && Objects.equals(this.replicaProvisionedReadCapacityAutoScalingSettings, that.replicaProvisionedReadCapacityAutoScalingSettings)
               && Objects.equals(this.replicaProvisionedWriteCapacityAutoScalingSettings, that.replicaProvisionedWriteCapacityAutoScalingSettings)
               && Objects.equals(this.replicaTableClassSummary, that.replicaTableClassSummary)
               && Objects.equals(this.replicaGlobalSecondaryIndexSettings, that.replicaGlobalSecondaryIndexSettings);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(regionName);
        $hc = 31 * $hc + Objects.hashCode(replicaStatus);
        $hc = 31 * $hc + Objects.hashCode(replicaBillingModeSummary);
        $hc = 31 * $hc + Objects.hashCode(replicaProvisionedReadCapacityUnits);
        $hc = 31 * $hc + Objects.hashCode(replicaProvisionedReadCapacityAutoScalingSettings);
        $hc = 31 * $hc + Objects.hashCode(replicaProvisionedWriteCapacityUnits);
        $hc = 31 * $hc + Objects.hashCode(replicaProvisionedWriteCapacityAutoScalingSettings);
        $hc = 31 * $hc + Objects.hashCode(replicaGlobalSecondaryIndexSettings);
        $hc = 31 * $hc + Objects.hashCode(replicaTableClassSummary);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_REGION_NAME, regionName);
        if (replicaStatus != null) {
            serializer.writeString($SCHEMA_REPLICA_STATUS, replicaStatus.getValue());
        }
        if (replicaBillingModeSummary != null) {
            serializer.writeStruct($SCHEMA_REPLICA_BILLING_MODE_SUMMARY, replicaBillingModeSummary);
        }
        if (replicaProvisionedReadCapacityUnits != null) {
            serializer.writeLong($SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_UNITS, replicaProvisionedReadCapacityUnits);
        }
        if (replicaProvisionedReadCapacityAutoScalingSettings != null) {
            serializer.writeStruct($SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS, replicaProvisionedReadCapacityAutoScalingSettings);
        }
        if (replicaProvisionedWriteCapacityUnits != null) {
            serializer.writeLong($SCHEMA_REPLICA_PROVISIONED_WRITE_CAPACITY_UNITS, replicaProvisionedWriteCapacityUnits);
        }
        if (replicaProvisionedWriteCapacityAutoScalingSettings != null) {
            serializer.writeStruct($SCHEMA_REPLICA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_SETTINGS, replicaProvisionedWriteCapacityAutoScalingSettings);
        }
        if (replicaGlobalSecondaryIndexSettings != null) {
            serializer.writeList($SCHEMA_REPLICA_GLOBAL_SECONDARY_INDEX_SETTINGS, replicaGlobalSecondaryIndexSettings, replicaGlobalSecondaryIndexSettings.size(), SharedSerde.ReplicaGlobalSecondaryIndexSettingsDescriptionListSerializer.INSTANCE);
        }
        if (replicaTableClassSummary != null) {
            serializer.writeStruct($SCHEMA_REPLICA_TABLE_CLASS_SUMMARY, replicaTableClassSummary);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_REGION_NAME, member, regionName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_STATUS, member, replicaStatus);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_BILLING_MODE_SUMMARY, member, replicaBillingModeSummary);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_UNITS, member, replicaProvisionedReadCapacityUnits);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS, member, replicaProvisionedReadCapacityAutoScalingSettings);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_PROVISIONED_WRITE_CAPACITY_UNITS, member, replicaProvisionedWriteCapacityUnits);
            case 6 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_SETTINGS, member, replicaProvisionedWriteCapacityAutoScalingSettings);
            case 7 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_GLOBAL_SECONDARY_INDEX_SETTINGS, member, replicaGlobalSecondaryIndexSettings);
            case 8 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_TABLE_CLASS_SUMMARY, member, replicaTableClassSummary);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ReplicaSettingsDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.regionName(this.regionName);
        builder.replicaStatus(this.replicaStatus);
        builder.replicaBillingModeSummary(this.replicaBillingModeSummary);
        builder.replicaProvisionedReadCapacityUnits(this.replicaProvisionedReadCapacityUnits);
        builder.replicaProvisionedReadCapacityAutoScalingSettings(this.replicaProvisionedReadCapacityAutoScalingSettings);
        builder.replicaProvisionedWriteCapacityUnits(this.replicaProvisionedWriteCapacityUnits);
        builder.replicaProvisionedWriteCapacityAutoScalingSettings(this.replicaProvisionedWriteCapacityAutoScalingSettings);
        builder.replicaGlobalSecondaryIndexSettings(this.replicaGlobalSecondaryIndexSettings);
        builder.replicaTableClassSummary(this.replicaTableClassSummary);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ReplicaSettingsDescription}.
     */
    public static final class Builder implements ShapeBuilder<ReplicaSettingsDescription> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String regionName;
        private ReplicaStatus replicaStatus;
        private BillingModeSummary replicaBillingModeSummary;
        private Long replicaProvisionedReadCapacityUnits;
        private AutoScalingSettingsDescription replicaProvisionedReadCapacityAutoScalingSettings;
        private Long replicaProvisionedWriteCapacityUnits;
        private AutoScalingSettingsDescription replicaProvisionedWriteCapacityAutoScalingSettings;
        private List<ReplicaGlobalSecondaryIndexSettingsDescription> replicaGlobalSecondaryIndexSettings;
        private TableClassSummary replicaTableClassSummary;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The Region name of the replica.
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
         * The current state of the Region:
         *
         * <ul>
         *   <li>
         *     <code>CREATING</code> - The Region is being created.
         *   </li>
         *   <li>
         *     <code>UPDATING</code> - The Region is being updated.
         *   </li>
         *   <li>
         *     <code>DELETING</code> - The Region is being deleted.
         *   </li>
         *   <li>
         *     <code>ACTIVE</code> - The Region is ready for use.
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
         * The read/write capacity mode of the replica.
         *
         * @return this builder.
         */
        public Builder replicaBillingModeSummary(BillingModeSummary replicaBillingModeSummary) {
            this.replicaBillingModeSummary = replicaBillingModeSummary;
            return this;
        }

        /**
         * The maximum number of strongly consistent reads consumed per second before DynamoDB returns a <code>
         * ThrottlingException</code>. For more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/WorkingWithTables.html#ProvisionedThroughput">Specifying Read and Write Requirements</a> in the <i>
         * Amazon DynamoDB Developer Guide</i>.
         *
         * @return this builder.
         */
        public Builder replicaProvisionedReadCapacityUnits(Long replicaProvisionedReadCapacityUnits) {
            this.replicaProvisionedReadCapacityUnits = replicaProvisionedReadCapacityUnits;
            return this;
        }

        /**
         * Auto scaling settings for a global table replica's read capacity units.
         *
         * @return this builder.
         */
        public Builder replicaProvisionedReadCapacityAutoScalingSettings(AutoScalingSettingsDescription replicaProvisionedReadCapacityAutoScalingSettings) {
            this.replicaProvisionedReadCapacityAutoScalingSettings = replicaProvisionedReadCapacityAutoScalingSettings;
            return this;
        }

        /**
         * The maximum number of writes consumed per second before DynamoDB returns a <code>ThrottlingException</code>. For
         * more information, see <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/WorkingWithTables.html#ProvisionedThroughput">Specifying Read and Write Requirements</a> in the <i>Amazon DynamoDB Developer Guide</i>
         * .
         *
         * @return this builder.
         */
        public Builder replicaProvisionedWriteCapacityUnits(Long replicaProvisionedWriteCapacityUnits) {
            this.replicaProvisionedWriteCapacityUnits = replicaProvisionedWriteCapacityUnits;
            return this;
        }

        /**
         * Auto scaling settings for a global table replica's write capacity units.
         *
         * @return this builder.
         */
        public Builder replicaProvisionedWriteCapacityAutoScalingSettings(AutoScalingSettingsDescription replicaProvisionedWriteCapacityAutoScalingSettings) {
            this.replicaProvisionedWriteCapacityAutoScalingSettings = replicaProvisionedWriteCapacityAutoScalingSettings;
            return this;
        }

        /**
         * Replica global secondary index settings for the global table.
         *
         * @return this builder.
         */
        public Builder replicaGlobalSecondaryIndexSettings(List<ReplicaGlobalSecondaryIndexSettingsDescription> replicaGlobalSecondaryIndexSettings) {
            this.replicaGlobalSecondaryIndexSettings = replicaGlobalSecondaryIndexSettings;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder replicaTableClassSummary(TableClassSummary replicaTableClassSummary) {
            this.replicaTableClassSummary = replicaTableClassSummary;
            return this;
        }

        @Override
        public ReplicaSettingsDescription build() {
            tracker.validate();
            return new ReplicaSettingsDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> regionName((String) SchemaUtils.validateSameMember($SCHEMA_REGION_NAME, member, value));
                case 1 -> replicaStatus((ReplicaStatus) SchemaUtils.validateSameMember($SCHEMA_REPLICA_STATUS, member, value));
                case 2 -> replicaBillingModeSummary((BillingModeSummary) SchemaUtils.validateSameMember($SCHEMA_REPLICA_BILLING_MODE_SUMMARY, member, value));
                case 3 -> replicaProvisionedReadCapacityUnits((Long) SchemaUtils.validateSameMember($SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_UNITS, member, value));
                case 4 -> replicaProvisionedReadCapacityAutoScalingSettings((AutoScalingSettingsDescription) SchemaUtils.validateSameMember($SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS, member, value));
                case 5 -> replicaProvisionedWriteCapacityUnits((Long) SchemaUtils.validateSameMember($SCHEMA_REPLICA_PROVISIONED_WRITE_CAPACITY_UNITS, member, value));
                case 6 -> replicaProvisionedWriteCapacityAutoScalingSettings((AutoScalingSettingsDescription) SchemaUtils.validateSameMember($SCHEMA_REPLICA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_SETTINGS, member, value));
                case 7 -> replicaGlobalSecondaryIndexSettings((List<ReplicaGlobalSecondaryIndexSettingsDescription>) SchemaUtils.validateSameMember($SCHEMA_REPLICA_GLOBAL_SECONDARY_INDEX_SETTINGS, member, value));
                case 8 -> replicaTableClassSummary((TableClassSummary) SchemaUtils.validateSameMember($SCHEMA_REPLICA_TABLE_CLASS_SUMMARY, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ReplicaSettingsDescription> errorCorrection() {
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
                    case 1 -> builder.replicaStatus(ReplicaStatus.builder().deserializeMember(de, member).build());
                    case 2 -> builder.replicaBillingModeSummary(BillingModeSummary.builder().deserializeMember(de, member).build());
                    case 3 -> builder.replicaProvisionedReadCapacityUnits(de.readLong(member));
                    case 4 -> builder.replicaProvisionedReadCapacityAutoScalingSettings(AutoScalingSettingsDescription.builder().deserializeMember(de, member).build());
                    case 5 -> builder.replicaProvisionedWriteCapacityUnits(de.readLong(member));
                    case 6 -> builder.replicaProvisionedWriteCapacityAutoScalingSettings(AutoScalingSettingsDescription.builder().deserializeMember(de, member).build());
                    case 7 -> builder.replicaGlobalSecondaryIndexSettings(SharedSerde.deserializeReplicaGlobalSecondaryIndexSettingsDescriptionList(member, de));
                    case 8 -> builder.replicaTableClassSummary(TableClassSummary.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
