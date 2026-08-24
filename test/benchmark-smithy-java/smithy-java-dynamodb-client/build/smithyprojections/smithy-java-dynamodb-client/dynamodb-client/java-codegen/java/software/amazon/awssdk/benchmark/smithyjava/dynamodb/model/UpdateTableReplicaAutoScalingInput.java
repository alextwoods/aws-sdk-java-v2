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
public final class UpdateTableReplicaAutoScalingInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.UPDATE_TABLE_REPLICA_AUTO_SCALING_INPUT;
    private static final Schema $SCHEMA_GLOBAL_SECONDARY_INDEX_UPDATES = $SCHEMA.member("GlobalSecondaryIndexUpdates");
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_UPDATE = $SCHEMA.member("ProvisionedWriteCapacityAutoScalingUpdate");
    private static final Schema $SCHEMA_REPLICA_UPDATES = $SCHEMA.member("ReplicaUpdates");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient List<GlobalSecondaryIndexAutoScalingUpdate> globalSecondaryIndexUpdates;
    private final transient String tableName;
    private final transient AutoScalingSettingsUpdate provisionedWriteCapacityAutoScalingUpdate;
    private final transient List<ReplicaAutoScalingUpdate> replicaUpdates;

    private UpdateTableReplicaAutoScalingInput(Builder builder) {
        this.globalSecondaryIndexUpdates = builder.globalSecondaryIndexUpdates == null ? null : Collections.unmodifiableList(builder.globalSecondaryIndexUpdates);
        this.tableName = builder.tableName;
        this.provisionedWriteCapacityAutoScalingUpdate = builder.provisionedWriteCapacityAutoScalingUpdate;
        this.replicaUpdates = builder.replicaUpdates == null ? null : Collections.unmodifiableList(builder.replicaUpdates);
    }

    /**
     * Represents the auto scaling settings of the global secondary indexes of the replica to be updated.
     */
    public List<GlobalSecondaryIndexAutoScalingUpdate> getGlobalSecondaryIndexUpdates() {
        if (globalSecondaryIndexUpdates == null) {
            return Collections.emptyList();
        }
        return globalSecondaryIndexUpdates;
    }

    public boolean hasGlobalSecondaryIndexUpdates() {
        return globalSecondaryIndexUpdates != null;
    }

    /**
     * The name of the global table to be updated. You can also provide the Amazon Resource Name (ARN) of the table in
     * this parameter.
     */
    public String getTableName() {
        return tableName;
    }

    public AutoScalingSettingsUpdate getProvisionedWriteCapacityAutoScalingUpdate() {
        return provisionedWriteCapacityAutoScalingUpdate;
    }

    /**
     * Represents the auto scaling settings of replicas of the table that will be modified.
     */
    public List<ReplicaAutoScalingUpdate> getReplicaUpdates() {
        if (replicaUpdates == null) {
            return Collections.emptyList();
        }
        return replicaUpdates;
    }

    public boolean hasReplicaUpdates() {
        return replicaUpdates != null;
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
        UpdateTableReplicaAutoScalingInput that = (UpdateTableReplicaAutoScalingInput) other;
        return Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.provisionedWriteCapacityAutoScalingUpdate, that.provisionedWriteCapacityAutoScalingUpdate)
               && Objects.equals(this.globalSecondaryIndexUpdates, that.globalSecondaryIndexUpdates)
               && Objects.equals(this.replicaUpdates, that.replicaUpdates);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(globalSecondaryIndexUpdates);
        $hc = 31 * $hc + Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(provisionedWriteCapacityAutoScalingUpdate);
        $hc = 31 * $hc + Objects.hashCode(replicaUpdates);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (globalSecondaryIndexUpdates != null) {
            serializer.writeList($SCHEMA_GLOBAL_SECONDARY_INDEX_UPDATES, globalSecondaryIndexUpdates, globalSecondaryIndexUpdates.size(), SharedSerde.GlobalSecondaryIndexAutoScalingUpdateListSerializer.INSTANCE);
        }
        serializer.writeString($SCHEMA_TABLE_NAME, tableName);
        if (provisionedWriteCapacityAutoScalingUpdate != null) {
            serializer.writeStruct($SCHEMA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_UPDATE, provisionedWriteCapacityAutoScalingUpdate);
        }
        if (replicaUpdates != null) {
            serializer.writeList($SCHEMA_REPLICA_UPDATES, replicaUpdates, replicaUpdates.size(), SharedSerde.ReplicaAutoScalingUpdateListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_SECONDARY_INDEX_UPDATES, member, globalSecondaryIndexUpdates);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_UPDATE, member, provisionedWriteCapacityAutoScalingUpdate);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICA_UPDATES, member, replicaUpdates);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link UpdateTableReplicaAutoScalingInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.globalSecondaryIndexUpdates(this.globalSecondaryIndexUpdates);
        builder.tableName(this.tableName);
        builder.provisionedWriteCapacityAutoScalingUpdate(this.provisionedWriteCapacityAutoScalingUpdate);
        builder.replicaUpdates(this.replicaUpdates);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link UpdateTableReplicaAutoScalingInput}.
     */
    public static final class Builder implements ShapeBuilder<UpdateTableReplicaAutoScalingInput> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private List<GlobalSecondaryIndexAutoScalingUpdate> globalSecondaryIndexUpdates;
        private String tableName;
        private AutoScalingSettingsUpdate provisionedWriteCapacityAutoScalingUpdate;
        private List<ReplicaAutoScalingUpdate> replicaUpdates;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Represents the auto scaling settings of the global secondary indexes of the replica to be updated.
         *
         * @return this builder.
         */
        public Builder globalSecondaryIndexUpdates(List<GlobalSecondaryIndexAutoScalingUpdate> globalSecondaryIndexUpdates) {
            this.globalSecondaryIndexUpdates = globalSecondaryIndexUpdates;
            return this;
        }

        /**
         * The name of the global table to be updated. You can also provide the Amazon Resource Name (ARN) of the table in
         * this parameter.
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
         * @return this builder.
         */
        public Builder provisionedWriteCapacityAutoScalingUpdate(AutoScalingSettingsUpdate provisionedWriteCapacityAutoScalingUpdate) {
            this.provisionedWriteCapacityAutoScalingUpdate = provisionedWriteCapacityAutoScalingUpdate;
            return this;
        }

        /**
         * Represents the auto scaling settings of replicas of the table that will be modified.
         *
         * @return this builder.
         */
        public Builder replicaUpdates(List<ReplicaAutoScalingUpdate> replicaUpdates) {
            this.replicaUpdates = replicaUpdates;
            return this;
        }

        @Override
        public UpdateTableReplicaAutoScalingInput build() {
            tracker.validate();
            return new UpdateTableReplicaAutoScalingInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 1 -> globalSecondaryIndexUpdates((List<GlobalSecondaryIndexAutoScalingUpdate>) SchemaUtils.validateSameMember($SCHEMA_GLOBAL_SECONDARY_INDEX_UPDATES, member, value));
                case 2 -> provisionedWriteCapacityAutoScalingUpdate((AutoScalingSettingsUpdate) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_UPDATE, member, value));
                case 3 -> replicaUpdates((List<ReplicaAutoScalingUpdate>) SchemaUtils.validateSameMember($SCHEMA_REPLICA_UPDATES, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<UpdateTableReplicaAutoScalingInput> errorCorrection() {
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
                    case 1 -> builder.globalSecondaryIndexUpdates(SharedSerde.deserializeGlobalSecondaryIndexAutoScalingUpdateList(member, de));
                    case 2 -> builder.provisionedWriteCapacityAutoScalingUpdate(AutoScalingSettingsUpdate.builder().deserializeMember(de, member).build());
                    case 3 -> builder.replicaUpdates(SharedSerde.deserializeReplicaAutoScalingUpdateList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
