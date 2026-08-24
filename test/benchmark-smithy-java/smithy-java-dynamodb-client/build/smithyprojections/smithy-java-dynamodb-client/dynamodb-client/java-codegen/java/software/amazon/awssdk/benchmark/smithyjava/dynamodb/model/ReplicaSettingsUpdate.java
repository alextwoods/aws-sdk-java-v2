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
 * Represents the settings for a global table in a Region that will be modified.
 */
@SmithyGenerated
public final class ReplicaSettingsUpdate implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.REPLICA_SETTINGS_UPDATE;
    private static final Schema $SCHEMA_REGION_NAME = $SCHEMA.member("RegionName");
    private static final Schema $SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_UNITS = $SCHEMA.member("ReplicaProvisionedReadCapacityUnits");
    private static final Schema $SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS_UPDATE = $SCHEMA.member("ReplicaProvisionedReadCapacityAutoScalingSettingsUpdate");
    private static final Schema $SCHEMA_REPLICA_GLOBAL_SECONDARY_INDEX_SETTINGS_UPDATE = $SCHEMA.member("ReplicaGlobalSecondaryIndexSettingsUpdate");
    private static final Schema $SCHEMA_REPLICA_TABLE_CLASS = $SCHEMA.member("ReplicaTableClass");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String regionName;
    private final transient Long replicaProvisionedReadCapacityUnits;
    private final transient AutoScalingSettingsUpdate replicaProvisionedReadCapacityAutoScalingSettingsUpdate;
    private final transient List<ReplicaGlobalSecondaryIndexSettingsUpdate> replicaGlobalSecondaryIndexSettingsUpdate;
    private final transient TableClass replicaTableClass;

    private ReplicaSettingsUpdate(Builder builder) {
        this.regionName = builder.regionName;
        this.replicaProvisionedReadCapacityUnits = builder.replicaProvisionedReadCapacityUnits;
        this.replicaProvisionedReadCapacityAutoScalingSettingsUpdate = builder.replicaProvisionedReadCapacityAutoScalingSettingsUpdate;
        this.replicaGlobalSecondaryIndexSettingsUpdate = builder.replicaGlobalSecondaryIndexSettingsUpdate == null ? null : Collections.unmodifiableList(builder.replicaGlobalSecondaryIndexSettingsUpdate);
        this.replicaTableClass = builder.replicaTableClass;
    }

    /**
     * The Region of the replica to be added.
     */
    public String getRegionName() {
        return regionName;
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
     * Auto scaling settings for managing a global table replica's read capacity units.
     */
    public AutoScalingSettingsUpdate getReplicaProvisionedReadCapacityAutoScalingSettingsUpdate() {
        return replicaProvisionedReadCapacityAutoScalingSettingsUpdate;
    }

    /**
     * Represents the settings of a global secondary index for a global table that will be modified.
     */
    public List<ReplicaGlobalSecondaryIndexSettingsUpdate> getReplicaGlobalSecondaryIndexSettingsUpdate() {
        if (replicaGlobalSecondaryIndexSettingsUpdate == null) {
            return Collections.emptyList();
        }
        return replicaGlobalSecondaryIndexSettingsUpdate;
    }

    public boolean hasReplicaGlobalSecondaryIndexSettingsUpdate() {
        return replicaGlobalSecondaryIndexSettingsUpdate != null;
    }

    /**
     * Replica-specific table class. If not specified, uses the source table's table class.
     */
    public TableClass getReplicaTableClass() {
        return replicaTableClass;
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
        ReplicaSettingsUpdate that = (ReplicaSettingsUpdate) other;
        return Objects.equals(this.replicaProvisionedReadCapacityUnits, that.replicaProvisionedReadCapacityUnits)
               && Objects.equals(this.regionName, that.regionName)
               && Objects.equals(this.replicaTableClass, that.replicaTableClass)
               && Objects.equals(this.replicaProvisionedReadCapacityAutoScalingSettingsUpdate, that.replicaProvisionedReadCapacityAutoScalingSettingsUpdate)
               && Objects.equals(this.replicaGlobalSecondaryIndexSettingsUpdate, that.replicaGlobalSecondaryIndexSettingsUpdate);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(regionName);
        $hc = 31 * $hc + Objects.hashCode(replicaProvisionedReadCapacityUnits);
        $hc = 31 * $hc + Objects.hashCode(replicaProvisionedReadCapacityAutoScalingSettingsUpdate);
        $hc = 31 * $hc + Objects.hashCode(replicaGlobalSecondaryIndexSettingsUpdate);
        $hc = 31 * $hc + Objects.hashCode(replicaTableClass);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_REGION_NAME, regionName);
        if (replicaProvisionedReadCapacityUnits != null) {
            serializer.writeLong($SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_UNITS, replicaProvisionedReadCapacityUnits);
        }
        if (replicaProvisionedReadCapacityAutoScalingSettingsUpdate != null) {
            serializer.writeStruct($SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS_UPDATE, replicaProvisionedReadCapacityAutoScalingSettingsUpdate);
        }
        if (replicaGlobalSecondaryIndexSettingsUpdate != null) {
            serializer.writeList($SCHEMA_REPLICA_GLOBAL_SECONDARY_INDEX_SETTINGS_UPDATE, replicaGlobalSecondaryIndexSettingsUpdate, replicaGlobalSecondaryIndexSettingsUpdate.size(), SharedSerde.ReplicaGlobalSecondaryIndexSettingsUpdateListSerializer.INSTANCE);
        }
        if (replicaTableClass != null) {
            serializer.writeString($SCHEMA_REPLICA_TABLE_CLASS, replicaTableClass.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_REGION_NAME, member, regionName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_UNITS, member, replicaProvisionedReadCapacityUnits);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS_UPDATE, member, replicaProvisionedReadCapacityAutoScalingSettingsUpdate);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_GLOBAL_SECONDARY_INDEX_SETTINGS_UPDATE, member, replicaGlobalSecondaryIndexSettingsUpdate);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_TABLE_CLASS, member, replicaTableClass);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ReplicaSettingsUpdate}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.regionName(this.regionName);
        builder.replicaProvisionedReadCapacityUnits(this.replicaProvisionedReadCapacityUnits);
        builder.replicaProvisionedReadCapacityAutoScalingSettingsUpdate(this.replicaProvisionedReadCapacityAutoScalingSettingsUpdate);
        builder.replicaGlobalSecondaryIndexSettingsUpdate(this.replicaGlobalSecondaryIndexSettingsUpdate);
        builder.replicaTableClass(this.replicaTableClass);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ReplicaSettingsUpdate}.
     */
    public static final class Builder implements ShapeBuilder<ReplicaSettingsUpdate> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String regionName;
        private Long replicaProvisionedReadCapacityUnits;
        private AutoScalingSettingsUpdate replicaProvisionedReadCapacityAutoScalingSettingsUpdate;
        private List<ReplicaGlobalSecondaryIndexSettingsUpdate> replicaGlobalSecondaryIndexSettingsUpdate;
        private TableClass replicaTableClass;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The Region of the replica to be added.
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
         * Auto scaling settings for managing a global table replica's read capacity units.
         *
         * @return this builder.
         */
        public Builder replicaProvisionedReadCapacityAutoScalingSettingsUpdate(AutoScalingSettingsUpdate replicaProvisionedReadCapacityAutoScalingSettingsUpdate) {
            this.replicaProvisionedReadCapacityAutoScalingSettingsUpdate = replicaProvisionedReadCapacityAutoScalingSettingsUpdate;
            return this;
        }

        /**
         * Represents the settings of a global secondary index for a global table that will be modified.
         *
         * @return this builder.
         */
        public Builder replicaGlobalSecondaryIndexSettingsUpdate(List<ReplicaGlobalSecondaryIndexSettingsUpdate> replicaGlobalSecondaryIndexSettingsUpdate) {
            this.replicaGlobalSecondaryIndexSettingsUpdate = replicaGlobalSecondaryIndexSettingsUpdate;
            return this;
        }

        /**
         * Replica-specific table class. If not specified, uses the source table's table class.
         *
         * @return this builder.
         */
        public Builder replicaTableClass(TableClass replicaTableClass) {
            this.replicaTableClass = replicaTableClass;
            return this;
        }

        @Override
        public ReplicaSettingsUpdate build() {
            tracker.validate();
            return new ReplicaSettingsUpdate(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> regionName((String) SchemaUtils.validateSameMember($SCHEMA_REGION_NAME, member, value));
                case 1 -> replicaProvisionedReadCapacityUnits((Long) SchemaUtils.validateSameMember($SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_UNITS, member, value));
                case 2 -> replicaProvisionedReadCapacityAutoScalingSettingsUpdate((AutoScalingSettingsUpdate) SchemaUtils.validateSameMember($SCHEMA_REPLICA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS_UPDATE, member, value));
                case 3 -> replicaGlobalSecondaryIndexSettingsUpdate((List<ReplicaGlobalSecondaryIndexSettingsUpdate>) SchemaUtils.validateSameMember($SCHEMA_REPLICA_GLOBAL_SECONDARY_INDEX_SETTINGS_UPDATE, member, value));
                case 4 -> replicaTableClass((TableClass) SchemaUtils.validateSameMember($SCHEMA_REPLICA_TABLE_CLASS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ReplicaSettingsUpdate> errorCorrection() {
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
                    case 1 -> builder.replicaProvisionedReadCapacityUnits(de.readLong(member));
                    case 2 -> builder.replicaProvisionedReadCapacityAutoScalingSettingsUpdate(AutoScalingSettingsUpdate.builder().deserializeMember(de, member).build());
                    case 3 -> builder.replicaGlobalSecondaryIndexSettingsUpdate(SharedSerde.deserializeReplicaGlobalSecondaryIndexSettingsUpdateList(member, de));
                    case 4 -> builder.replicaTableClass(TableClass.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
