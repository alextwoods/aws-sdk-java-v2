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
 * Represents the auto scaling settings of a replica that will be modified.
 */
@SmithyGenerated
public final class ReplicaAutoScalingUpdate implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.REPLICA_AUTO_SCALING_UPDATE;
    private static final Schema $SCHEMA_REGION_NAME = $SCHEMA.member("RegionName");
    private static final Schema $SCHEMA_REPLICA_GLOBAL_SECONDARY_INDEX_UPDATES = $SCHEMA.member("ReplicaGlobalSecondaryIndexUpdates");
    private static final Schema $SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_UPDATE = $SCHEMA.member("ReplicaProvisionedReadCapacityAutoScalingUpdate");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String regionName;
    private final transient List<ReplicaGlobalSecondaryIndexAutoScalingUpdate> replicaGlobalSecondaryIndexUpdates;
    private final transient AutoScalingSettingsUpdate replicaProvisionedReadCapacityAutoScalingUpdate;

    private ReplicaAutoScalingUpdate(Builder builder) {
        this.regionName = builder.regionName;
        this.replicaGlobalSecondaryIndexUpdates = builder.replicaGlobalSecondaryIndexUpdates == null ? null : Collections.unmodifiableList(builder.replicaGlobalSecondaryIndexUpdates);
        this.replicaProvisionedReadCapacityAutoScalingUpdate = builder.replicaProvisionedReadCapacityAutoScalingUpdate;
    }

    /**
     * The Region where the replica exists.
     */
    public String getRegionName() {
        return regionName;
    }

    /**
     * Represents the auto scaling settings of global secondary indexes that will be modified.
     */
    public List<ReplicaGlobalSecondaryIndexAutoScalingUpdate> getReplicaGlobalSecondaryIndexUpdates() {
        if (replicaGlobalSecondaryIndexUpdates == null) {
            return Collections.emptyList();
        }
        return replicaGlobalSecondaryIndexUpdates;
    }

    public boolean hasReplicaGlobalSecondaryIndexUpdates() {
        return replicaGlobalSecondaryIndexUpdates != null;
    }

    public AutoScalingSettingsUpdate getReplicaProvisionedReadCapacityAutoScalingUpdate() {
        return replicaProvisionedReadCapacityAutoScalingUpdate;
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
        ReplicaAutoScalingUpdate that = (ReplicaAutoScalingUpdate) other;
        return Objects.equals(this.regionName, that.regionName)
               && Objects.equals(this.replicaProvisionedReadCapacityAutoScalingUpdate, that.replicaProvisionedReadCapacityAutoScalingUpdate)
               && Objects.equals(this.replicaGlobalSecondaryIndexUpdates, that.replicaGlobalSecondaryIndexUpdates);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(regionName);
        $hc = 31 * $hc + Objects.hashCode(replicaGlobalSecondaryIndexUpdates);
        $hc = 31 * $hc + Objects.hashCode(replicaProvisionedReadCapacityAutoScalingUpdate);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_REGION_NAME, regionName);
        if (replicaGlobalSecondaryIndexUpdates != null) {
            serializer.writeList($SCHEMA_REPLICA_GLOBAL_SECONDARY_INDEX_UPDATES, replicaGlobalSecondaryIndexUpdates, replicaGlobalSecondaryIndexUpdates.size(), SharedSerde.ReplicaGlobalSecondaryIndexAutoScalingUpdateListSerializer.INSTANCE);
        }
        if (replicaProvisionedReadCapacityAutoScalingUpdate != null) {
            serializer.writeStruct($SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_UPDATE, replicaProvisionedReadCapacityAutoScalingUpdate);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_REGION_NAME, member, regionName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_GLOBAL_SECONDARY_INDEX_UPDATES, member, replicaGlobalSecondaryIndexUpdates);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_UPDATE, member, replicaProvisionedReadCapacityAutoScalingUpdate);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ReplicaAutoScalingUpdate}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.regionName(this.regionName);
        builder.replicaGlobalSecondaryIndexUpdates(this.replicaGlobalSecondaryIndexUpdates);
        builder.replicaProvisionedReadCapacityAutoScalingUpdate(this.replicaProvisionedReadCapacityAutoScalingUpdate);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ReplicaAutoScalingUpdate}.
     */
    public static final class Builder implements ShapeBuilder<ReplicaAutoScalingUpdate> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String regionName;
        private List<ReplicaGlobalSecondaryIndexAutoScalingUpdate> replicaGlobalSecondaryIndexUpdates;
        private AutoScalingSettingsUpdate replicaProvisionedReadCapacityAutoScalingUpdate;

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
         * Represents the auto scaling settings of global secondary indexes that will be modified.
         *
         * @return this builder.
         */
        public Builder replicaGlobalSecondaryIndexUpdates(List<ReplicaGlobalSecondaryIndexAutoScalingUpdate> replicaGlobalSecondaryIndexUpdates) {
            this.replicaGlobalSecondaryIndexUpdates = replicaGlobalSecondaryIndexUpdates;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder replicaProvisionedReadCapacityAutoScalingUpdate(AutoScalingSettingsUpdate replicaProvisionedReadCapacityAutoScalingUpdate) {
            this.replicaProvisionedReadCapacityAutoScalingUpdate = replicaProvisionedReadCapacityAutoScalingUpdate;
            return this;
        }

        @Override
        public ReplicaAutoScalingUpdate build() {
            tracker.validate();
            return new ReplicaAutoScalingUpdate(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> regionName((String) SchemaUtils.validateSameMember($SCHEMA_REGION_NAME, member, value));
                case 1 -> replicaGlobalSecondaryIndexUpdates((List<ReplicaGlobalSecondaryIndexAutoScalingUpdate>) SchemaUtils.validateSameMember($SCHEMA_REPLICA_GLOBAL_SECONDARY_INDEX_UPDATES, member, value));
                case 2 -> replicaProvisionedReadCapacityAutoScalingUpdate((AutoScalingSettingsUpdate) SchemaUtils.validateSameMember($SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_UPDATE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ReplicaAutoScalingUpdate> errorCorrection() {
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
                    case 1 -> builder.replicaGlobalSecondaryIndexUpdates(SharedSerde.deserializeReplicaGlobalSecondaryIndexAutoScalingUpdateList(member, de));
                    case 2 -> builder.replicaProvisionedReadCapacityAutoScalingUpdate(AutoScalingSettingsUpdate.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
