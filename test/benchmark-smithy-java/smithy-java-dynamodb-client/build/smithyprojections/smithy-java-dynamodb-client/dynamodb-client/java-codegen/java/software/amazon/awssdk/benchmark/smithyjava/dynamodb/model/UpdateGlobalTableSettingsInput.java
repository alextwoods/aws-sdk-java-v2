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

@SmithyGenerated
public final class UpdateGlobalTableSettingsInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.UPDATE_GLOBAL_TABLE_SETTINGS_INPUT;
    private static final Schema $SCHEMA_GLOBAL_TABLE_NAME = $SCHEMA.member("GlobalTableName");
    private static final Schema $SCHEMA_GLOBAL_TABLE_BILLING_MODE = $SCHEMA.member("GlobalTableBillingMode");
    private static final Schema $SCHEMA_GLOBAL_TABLE_PROVISIONED_WRITE_CAPACITY_UNITS = $SCHEMA.member("GlobalTableProvisionedWriteCapacityUnits");
    private static final Schema $SCHEMA_GLOBAL_TABLE_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_SETTINGS_UPDATE = $SCHEMA.member("GlobalTableProvisionedWriteCapacityAutoScalingSettingsUpdate");
    private static final Schema $SCHEMA_GLOBAL_TABLE_GLOBAL_SECONDARY_INDEX_SETTINGS_UPDATE = $SCHEMA.member("GlobalTableGlobalSecondaryIndexSettingsUpdate");
    private static final Schema $SCHEMA_REPLICA_SETTINGS_UPDATE = $SCHEMA.member("ReplicaSettingsUpdate");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String globalTableName;
    private final transient BillingMode globalTableBillingMode;
    private final transient Long globalTableProvisionedWriteCapacityUnits;
    private final transient AutoScalingSettingsUpdate globalTableProvisionedWriteCapacityAutoScalingSettingsUpdate;
    private final transient List<GlobalTableGlobalSecondaryIndexSettingsUpdate> globalTableGlobalSecondaryIndexSettingsUpdate;
    private final transient List<ReplicaSettingsUpdate> replicaSettingsUpdate;

    private UpdateGlobalTableSettingsInput(Builder builder) {
        this.globalTableName = builder.globalTableName;
        this.globalTableBillingMode = builder.globalTableBillingMode;
        this.globalTableProvisionedWriteCapacityUnits = builder.globalTableProvisionedWriteCapacityUnits;
        this.globalTableProvisionedWriteCapacityAutoScalingSettingsUpdate = builder.globalTableProvisionedWriteCapacityAutoScalingSettingsUpdate;
        this.globalTableGlobalSecondaryIndexSettingsUpdate = builder.globalTableGlobalSecondaryIndexSettingsUpdate == null ? null : Collections.unmodifiableList(builder.globalTableGlobalSecondaryIndexSettingsUpdate);
        this.replicaSettingsUpdate = builder.replicaSettingsUpdate == null ? null : Collections.unmodifiableList(builder.replicaSettingsUpdate);
    }

    /**
     * The name of the global table
     */
    public String getGlobalTableName() {
        return globalTableName;
    }

    /**
     * The billing mode of the global table. If <code>GlobalTableBillingMode</code> is not specified, the global table
     * defaults to <code>PROVISIONED</code> capacity billing mode.
     *
     * <ul>
     *   <li>
     *     <code>PROVISIONED</code> - We recommend using <code>PROVISIONED</code> for predictable workloads. <code>
     *     PROVISIONED</code> sets the billing mode to <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/provisioned-capacity-mode.html">Provisioned capacity mode</a>.
     *   </li>
     *   <li>
     *     <code>PAY_PER_REQUEST</code> - We recommend using <code>PAY_PER_REQUEST</code> for unpredictable
     *     workloads. <code>PAY_PER_REQUEST</code> sets the billing mode to <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/on-demand-capacity-mode.html">On-demand capacity mode</a>.
     *   </li>
     * </ul>
     */
    public BillingMode getGlobalTableBillingMode() {
        return globalTableBillingMode;
    }

    /**
     * The maximum number of writes consumed per second before DynamoDB returns a <code>ThrottlingException.</code>
     */
    public Long getGlobalTableProvisionedWriteCapacityUnits() {
        return globalTableProvisionedWriteCapacityUnits;
    }

    /**
     * Auto scaling settings for managing provisioned write capacity for the global table.
     */
    public AutoScalingSettingsUpdate getGlobalTableProvisionedWriteCapacityAutoScalingSettingsUpdate() {
        return globalTableProvisionedWriteCapacityAutoScalingSettingsUpdate;
    }

    /**
     * Represents the settings of a global secondary index for a global table that will be modified.
     */
    public List<GlobalTableGlobalSecondaryIndexSettingsUpdate> getGlobalTableGlobalSecondaryIndexSettingsUpdate() {
        if (globalTableGlobalSecondaryIndexSettingsUpdate == null) {
            return Collections.emptyList();
        }
        return globalTableGlobalSecondaryIndexSettingsUpdate;
    }

    public boolean hasGlobalTableGlobalSecondaryIndexSettingsUpdate() {
        return globalTableGlobalSecondaryIndexSettingsUpdate != null;
    }

    /**
     * Represents the settings for a global table in a Region that will be modified.
     */
    public List<ReplicaSettingsUpdate> getReplicaSettingsUpdate() {
        if (replicaSettingsUpdate == null) {
            return Collections.emptyList();
        }
        return replicaSettingsUpdate;
    }

    public boolean hasReplicaSettingsUpdate() {
        return replicaSettingsUpdate != null;
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
        UpdateGlobalTableSettingsInput that = (UpdateGlobalTableSettingsInput) other;
        return Objects.equals(this.globalTableProvisionedWriteCapacityUnits, that.globalTableProvisionedWriteCapacityUnits)
               && Objects.equals(this.globalTableName, that.globalTableName)
               && Objects.equals(this.globalTableBillingMode, that.globalTableBillingMode)
               && Objects.equals(this.globalTableProvisionedWriteCapacityAutoScalingSettingsUpdate, that.globalTableProvisionedWriteCapacityAutoScalingSettingsUpdate)
               && Objects.equals(this.globalTableGlobalSecondaryIndexSettingsUpdate, that.globalTableGlobalSecondaryIndexSettingsUpdate)
               && Objects.equals(this.replicaSettingsUpdate, that.replicaSettingsUpdate);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(globalTableName);
        $hc = 31 * $hc + Objects.hashCode(globalTableBillingMode);
        $hc = 31 * $hc + Objects.hashCode(globalTableProvisionedWriteCapacityUnits);
        $hc = 31 * $hc + Objects.hashCode(globalTableProvisionedWriteCapacityAutoScalingSettingsUpdate);
        $hc = 31 * $hc + Objects.hashCode(globalTableGlobalSecondaryIndexSettingsUpdate);
        $hc = 31 * $hc + Objects.hashCode(replicaSettingsUpdate);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_GLOBAL_TABLE_NAME, globalTableName);
        if (globalTableBillingMode != null) {
            serializer.writeString($SCHEMA_GLOBAL_TABLE_BILLING_MODE, globalTableBillingMode.getValue());
        }
        if (globalTableProvisionedWriteCapacityUnits != null) {
            serializer.writeLong($SCHEMA_GLOBAL_TABLE_PROVISIONED_WRITE_CAPACITY_UNITS, globalTableProvisionedWriteCapacityUnits);
        }
        if (globalTableProvisionedWriteCapacityAutoScalingSettingsUpdate != null) {
            serializer.writeStruct($SCHEMA_GLOBAL_TABLE_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_SETTINGS_UPDATE, globalTableProvisionedWriteCapacityAutoScalingSettingsUpdate);
        }
        if (globalTableGlobalSecondaryIndexSettingsUpdate != null) {
            serializer.writeList($SCHEMA_GLOBAL_TABLE_GLOBAL_SECONDARY_INDEX_SETTINGS_UPDATE, globalTableGlobalSecondaryIndexSettingsUpdate, globalTableGlobalSecondaryIndexSettingsUpdate.size(), SharedSerde.GlobalTableGlobalSecondaryIndexSettingsUpdateListSerializer.INSTANCE);
        }
        if (replicaSettingsUpdate != null) {
            serializer.writeList($SCHEMA_REPLICA_SETTINGS_UPDATE, replicaSettingsUpdate, replicaSettingsUpdate.size(), SharedSerde.ReplicaSettingsUpdateListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_NAME, member, globalTableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_BILLING_MODE, member, globalTableBillingMode);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_PROVISIONED_WRITE_CAPACITY_UNITS, member, globalTableProvisionedWriteCapacityUnits);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_SETTINGS_UPDATE, member, globalTableProvisionedWriteCapacityAutoScalingSettingsUpdate);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_GLOBAL_SECONDARY_INDEX_SETTINGS_UPDATE, member, globalTableGlobalSecondaryIndexSettingsUpdate);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_SETTINGS_UPDATE, member, replicaSettingsUpdate);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link UpdateGlobalTableSettingsInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.globalTableName(this.globalTableName);
        builder.globalTableBillingMode(this.globalTableBillingMode);
        builder.globalTableProvisionedWriteCapacityUnits(this.globalTableProvisionedWriteCapacityUnits);
        builder.globalTableProvisionedWriteCapacityAutoScalingSettingsUpdate(this.globalTableProvisionedWriteCapacityAutoScalingSettingsUpdate);
        builder.globalTableGlobalSecondaryIndexSettingsUpdate(this.globalTableGlobalSecondaryIndexSettingsUpdate);
        builder.replicaSettingsUpdate(this.replicaSettingsUpdate);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link UpdateGlobalTableSettingsInput}.
     */
    public static final class Builder implements ShapeBuilder<UpdateGlobalTableSettingsInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String globalTableName;
        private BillingMode globalTableBillingMode;
        private Long globalTableProvisionedWriteCapacityUnits;
        private AutoScalingSettingsUpdate globalTableProvisionedWriteCapacityAutoScalingSettingsUpdate;
        private List<GlobalTableGlobalSecondaryIndexSettingsUpdate> globalTableGlobalSecondaryIndexSettingsUpdate;
        private List<ReplicaSettingsUpdate> replicaSettingsUpdate;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the global table
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder globalTableName(String globalTableName) {
            this.globalTableName = Objects.requireNonNull(globalTableName, "globalTableName cannot be null");
            tracker.setMember($SCHEMA_GLOBAL_TABLE_NAME);
            return this;
        }

        /**
         * The billing mode of the global table. If <code>GlobalTableBillingMode</code> is not specified, the global table
         * defaults to <code>PROVISIONED</code> capacity billing mode.
         *
         * <ul>
         *   <li>
         *     <code>PROVISIONED</code> - We recommend using <code>PROVISIONED</code> for predictable workloads. <code>
         *     PROVISIONED</code> sets the billing mode to <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/provisioned-capacity-mode.html">Provisioned capacity mode</a>.
         *   </li>
         *   <li>
         *     <code>PAY_PER_REQUEST</code> - We recommend using <code>PAY_PER_REQUEST</code> for unpredictable
         *     workloads. <code>PAY_PER_REQUEST</code> sets the billing mode to <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/on-demand-capacity-mode.html">On-demand capacity mode</a>.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder globalTableBillingMode(BillingMode globalTableBillingMode) {
            this.globalTableBillingMode = globalTableBillingMode;
            return this;
        }

        /**
         * The maximum number of writes consumed per second before DynamoDB returns a <code>ThrottlingException.</code>
         *
         * @return this builder.
         */
        public Builder globalTableProvisionedWriteCapacityUnits(Long globalTableProvisionedWriteCapacityUnits) {
            this.globalTableProvisionedWriteCapacityUnits = globalTableProvisionedWriteCapacityUnits;
            return this;
        }

        /**
         * Auto scaling settings for managing provisioned write capacity for the global table.
         *
         * @return this builder.
         */
        public Builder globalTableProvisionedWriteCapacityAutoScalingSettingsUpdate(AutoScalingSettingsUpdate globalTableProvisionedWriteCapacityAutoScalingSettingsUpdate) {
            this.globalTableProvisionedWriteCapacityAutoScalingSettingsUpdate = globalTableProvisionedWriteCapacityAutoScalingSettingsUpdate;
            return this;
        }

        /**
         * Represents the settings of a global secondary index for a global table that will be modified.
         *
         * @return this builder.
         */
        public Builder globalTableGlobalSecondaryIndexSettingsUpdate(List<GlobalTableGlobalSecondaryIndexSettingsUpdate> globalTableGlobalSecondaryIndexSettingsUpdate) {
            this.globalTableGlobalSecondaryIndexSettingsUpdate = globalTableGlobalSecondaryIndexSettingsUpdate;
            return this;
        }

        /**
         * Represents the settings for a global table in a Region that will be modified.
         *
         * @return this builder.
         */
        public Builder replicaSettingsUpdate(List<ReplicaSettingsUpdate> replicaSettingsUpdate) {
            this.replicaSettingsUpdate = replicaSettingsUpdate;
            return this;
        }

        @Override
        public UpdateGlobalTableSettingsInput build() {
            tracker.validate();
            return new UpdateGlobalTableSettingsInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> globalTableName((String) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_NAME, member, value));
                case 1 -> globalTableBillingMode((BillingMode) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_BILLING_MODE, member, value));
                case 2 -> globalTableProvisionedWriteCapacityUnits((Long) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_PROVISIONED_WRITE_CAPACITY_UNITS, member, value));
                case 3 -> globalTableProvisionedWriteCapacityAutoScalingSettingsUpdate((AutoScalingSettingsUpdate) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_SETTINGS_UPDATE, member, value));
                case 4 -> globalTableGlobalSecondaryIndexSettingsUpdate((List<GlobalTableGlobalSecondaryIndexSettingsUpdate>) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_TABLE_GLOBAL_SECONDARY_INDEX_SETTINGS_UPDATE, member, value));
                case 5 -> replicaSettingsUpdate((List<ReplicaSettingsUpdate>) SchemaUtils.validateSameMember($SCHEMA_REPLICA_SETTINGS_UPDATE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<UpdateGlobalTableSettingsInput> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_GLOBAL_TABLE_NAME)) {
                globalTableName("");
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
                    case 0 -> builder.globalTableName(de.readString(member));
                    case 1 -> builder.globalTableBillingMode(BillingMode.builder().deserializeMember(de, member).build());
                    case 2 -> builder.globalTableProvisionedWriteCapacityUnits(de.readLong(member));
                    case 3 -> builder.globalTableProvisionedWriteCapacityAutoScalingSettingsUpdate(AutoScalingSettingsUpdate.builder().deserializeMember(de, member).build());
                    case 4 -> builder.globalTableGlobalSecondaryIndexSettingsUpdate(SharedSerde.deserializeGlobalTableGlobalSecondaryIndexSettingsUpdateList(member, de));
                    case 5 -> builder.replicaSettingsUpdate(SharedSerde.deserializeReplicaSettingsUpdateList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
