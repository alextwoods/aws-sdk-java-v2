package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
 * Represents the auto scaling settings of the replica.
 */
@SmithyGenerated
public final class ReplicaAutoScalingDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.REPLICA_AUTO_SCALING_DESCRIPTION;
    private static final Schema $SCHEMA_REGION_NAME = $SCHEMA.member("RegionName");
    private static final Schema $SCHEMA_GLOBAL_SECONDARY_INDEXES = $SCHEMA.member("GlobalSecondaryIndexes");
    private static final Schema $SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS = $SCHEMA.member("ReplicaProvisionedReadCapacityAutoScalingSettings");
    private static final Schema $SCHEMA_REPLICA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_SETTINGS = $SCHEMA.member("ReplicaProvisionedWriteCapacityAutoScalingSettings");
    private static final Schema $SCHEMA_REPLICA_STATUS = $SCHEMA.member("ReplicaStatus");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String regionName;
    private final transient List<ReplicaGlobalSecondaryIndexAutoScalingDescription> globalSecondaryIndexes;
    private final transient AutoScalingSettingsDescription replicaProvisionedReadCapacityAutoScalingSettings;
    private final transient AutoScalingSettingsDescription replicaProvisionedWriteCapacityAutoScalingSettings;
    private final transient ReplicaStatus replicaStatus;

    private ReplicaAutoScalingDescription(Builder builder) {
        this.regionName = builder.regionName;
        this.globalSecondaryIndexes = builder.globalSecondaryIndexes == null ? null : Collections.unmodifiableList(builder.globalSecondaryIndexes);
        this.replicaProvisionedReadCapacityAutoScalingSettings = builder.replicaProvisionedReadCapacityAutoScalingSettings;
        this.replicaProvisionedWriteCapacityAutoScalingSettings = builder.replicaProvisionedWriteCapacityAutoScalingSettings;
        this.replicaStatus = builder.replicaStatus;
    }

    /**
     * The Region where the replica exists.
     */
    public String getRegionName() {
        return regionName;
    }

    /**
     * Replica-specific global secondary index auto scaling settings.
     */
    public List<ReplicaGlobalSecondaryIndexAutoScalingDescription> getGlobalSecondaryIndexes() {
        if (globalSecondaryIndexes == null) {
            return Collections.emptyList();
        }
        return globalSecondaryIndexes;
    }

    public boolean hasGlobalSecondaryIndexes() {
        return globalSecondaryIndexes != null;
    }

    public AutoScalingSettingsDescription getReplicaProvisionedReadCapacityAutoScalingSettings() {
        return replicaProvisionedReadCapacityAutoScalingSettings;
    }

    public AutoScalingSettingsDescription getReplicaProvisionedWriteCapacityAutoScalingSettings() {
        return replicaProvisionedWriteCapacityAutoScalingSettings;
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
     * </ul>
     */
    public ReplicaStatus getReplicaStatus() {
        return replicaStatus;
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
        ReplicaAutoScalingDescription that = (ReplicaAutoScalingDescription) other;
        return Objects.equals(this.regionName, that.regionName)
               && Objects.equals(this.replicaStatus, that.replicaStatus)
               && Objects.equals(this.replicaProvisionedReadCapacityAutoScalingSettings, that.replicaProvisionedReadCapacityAutoScalingSettings)
               && Objects.equals(this.replicaProvisionedWriteCapacityAutoScalingSettings, that.replicaProvisionedWriteCapacityAutoScalingSettings)
               && Objects.equals(this.globalSecondaryIndexes, that.globalSecondaryIndexes);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(regionName);
        $hc = 31 * $hc + Objects.hashCode(globalSecondaryIndexes);
        $hc = 31 * $hc + Objects.hashCode(replicaProvisionedReadCapacityAutoScalingSettings);
        $hc = 31 * $hc + Objects.hashCode(replicaProvisionedWriteCapacityAutoScalingSettings);
        $hc = 31 * $hc + Objects.hashCode(replicaStatus);
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
        if (globalSecondaryIndexes != null) {
            serializer.writeList($SCHEMA_GLOBAL_SECONDARY_INDEXES, globalSecondaryIndexes, globalSecondaryIndexes.size(), SharedSerde.ReplicaGlobalSecondaryIndexAutoScalingDescriptionListSerializer.INSTANCE);
        }
        if (replicaProvisionedReadCapacityAutoScalingSettings != null) {
            serializer.writeStruct($SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS, replicaProvisionedReadCapacityAutoScalingSettings);
        }
        if (replicaProvisionedWriteCapacityAutoScalingSettings != null) {
            serializer.writeStruct($SCHEMA_REPLICA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_SETTINGS, replicaProvisionedWriteCapacityAutoScalingSettings);
        }
        if (replicaStatus != null) {
            serializer.writeString($SCHEMA_REPLICA_STATUS, replicaStatus.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_REGION_NAME, member, regionName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_SECONDARY_INDEXES, member, globalSecondaryIndexes);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS, member, replicaProvisionedReadCapacityAutoScalingSettings);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_SETTINGS, member, replicaProvisionedWriteCapacityAutoScalingSettings);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_STATUS, member, replicaStatus);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ReplicaAutoScalingDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.regionName(this.regionName);
        builder.globalSecondaryIndexes(this.globalSecondaryIndexes);
        builder.replicaProvisionedReadCapacityAutoScalingSettings(this.replicaProvisionedReadCapacityAutoScalingSettings);
        builder.replicaProvisionedWriteCapacityAutoScalingSettings(this.replicaProvisionedWriteCapacityAutoScalingSettings);
        builder.replicaStatus(this.replicaStatus);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ReplicaAutoScalingDescription}.
     */
    public static final class Builder implements ShapeBuilder<ReplicaAutoScalingDescription> {
        private String regionName;
        private List<ReplicaGlobalSecondaryIndexAutoScalingDescription> globalSecondaryIndexes;
        private AutoScalingSettingsDescription replicaProvisionedReadCapacityAutoScalingSettings;
        private AutoScalingSettingsDescription replicaProvisionedWriteCapacityAutoScalingSettings;
        private ReplicaStatus replicaStatus;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The Region where the replica exists.
         *
         * @return this builder.
         */
        public Builder regionName(String regionName) {
            this.regionName = regionName;
            return this;
        }

        /**
         * Replica-specific global secondary index auto scaling settings.
         *
         * @return this builder.
         */
        public Builder globalSecondaryIndexes(List<ReplicaGlobalSecondaryIndexAutoScalingDescription> globalSecondaryIndexes) {
            this.globalSecondaryIndexes = globalSecondaryIndexes;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder replicaProvisionedReadCapacityAutoScalingSettings(AutoScalingSettingsDescription replicaProvisionedReadCapacityAutoScalingSettings) {
            this.replicaProvisionedReadCapacityAutoScalingSettings = replicaProvisionedReadCapacityAutoScalingSettings;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder replicaProvisionedWriteCapacityAutoScalingSettings(AutoScalingSettingsDescription replicaProvisionedWriteCapacityAutoScalingSettings) {
            this.replicaProvisionedWriteCapacityAutoScalingSettings = replicaProvisionedWriteCapacityAutoScalingSettings;
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
         * </ul>
         *
         * @return this builder.
         */
        public Builder replicaStatus(ReplicaStatus replicaStatus) {
            this.replicaStatus = replicaStatus;
            return this;
        }

        @Override
        public ReplicaAutoScalingDescription build() {
            return new ReplicaAutoScalingDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> regionName((String) SchemaUtils.validateSameMember($SCHEMA_REGION_NAME, member, value));
                case 1 -> globalSecondaryIndexes((List<ReplicaGlobalSecondaryIndexAutoScalingDescription>) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_SECONDARY_INDEXES, member, value));
                case 2 -> replicaProvisionedReadCapacityAutoScalingSettings((AutoScalingSettingsDescription) SchemaUtils.validateSameMember($SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS, member, value));
                case 3 -> replicaProvisionedWriteCapacityAutoScalingSettings((AutoScalingSettingsDescription) SchemaUtils.validateSameMember($SCHEMA_REPLICA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_SETTINGS, member, value));
                case 4 -> replicaStatus((ReplicaStatus) SchemaUtils.validateSameMember($SCHEMA_REPLICA_STATUS, member, value));
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
                    case 1 -> builder.globalSecondaryIndexes(SharedSerde.deserializeReplicaGlobalSecondaryIndexAutoScalingDescriptionList(member, de));
                    case 2 -> builder.replicaProvisionedReadCapacityAutoScalingSettings(AutoScalingSettingsDescription.builder().deserializeMember(de, member).build());
                    case 3 -> builder.replicaProvisionedWriteCapacityAutoScalingSettings(AutoScalingSettingsDescription.builder().deserializeMember(de, member).build());
                    case 4 -> builder.replicaStatus(ReplicaStatus.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
