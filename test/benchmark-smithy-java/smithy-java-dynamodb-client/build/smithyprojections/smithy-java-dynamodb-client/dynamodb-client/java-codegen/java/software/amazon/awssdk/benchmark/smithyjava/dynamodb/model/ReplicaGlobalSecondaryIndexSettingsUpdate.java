package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
 * Represents the settings of a global secondary index for a global table that will be modified.
 */
@SmithyGenerated
public final class ReplicaGlobalSecondaryIndexSettingsUpdate implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_SETTINGS_UPDATE;
    private static final Schema $SCHEMA_INDEX_NAME = $SCHEMA.member("IndexName");
    private static final Schema $SCHEMA_PROVISIONED_READ_CAPACITY_UNITS = $SCHEMA.member("ProvisionedReadCapacityUnits");
    private static final Schema $SCHEMA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS_UPDATE = $SCHEMA.member("ProvisionedReadCapacityAutoScalingSettingsUpdate");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String indexName;
    private final transient Long provisionedReadCapacityUnits;
    private final transient AutoScalingSettingsUpdate provisionedReadCapacityAutoScalingSettingsUpdate;

    private ReplicaGlobalSecondaryIndexSettingsUpdate(Builder builder) {
        this.indexName = builder.indexName;
        this.provisionedReadCapacityUnits = builder.provisionedReadCapacityUnits;
        this.provisionedReadCapacityAutoScalingSettingsUpdate = builder.provisionedReadCapacityAutoScalingSettingsUpdate;
    }

    /**
     * The name of the global secondary index. The name must be unique among all other indexes on this table.
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * The maximum number of strongly consistent reads consumed per second before DynamoDB returns a
     * <code>ThrottlingException</code>.
     */
    public Long getProvisionedReadCapacityUnits() {
        return provisionedReadCapacityUnits;
    }

    /**
     * Auto scaling settings for managing a global secondary index replica's read capacity units.
     */
    public AutoScalingSettingsUpdate getProvisionedReadCapacityAutoScalingSettingsUpdate() {
        return provisionedReadCapacityAutoScalingSettingsUpdate;
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
        ReplicaGlobalSecondaryIndexSettingsUpdate that = (ReplicaGlobalSecondaryIndexSettingsUpdate) other;
        return Objects.equals(this.provisionedReadCapacityUnits, that.provisionedReadCapacityUnits)
               && Objects.equals(this.indexName, that.indexName)
               && Objects.equals(this.provisionedReadCapacityAutoScalingSettingsUpdate, that.provisionedReadCapacityAutoScalingSettingsUpdate);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(indexName);
        $hc = 31 * $hc + Objects.hashCode(provisionedReadCapacityUnits);
        $hc = 31 * $hc + Objects.hashCode(provisionedReadCapacityAutoScalingSettingsUpdate);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_INDEX_NAME, indexName);
        if (provisionedReadCapacityUnits != null) {
            serializer.writeLong($SCHEMA_PROVISIONED_READ_CAPACITY_UNITS, provisionedReadCapacityUnits);
        }
        if (provisionedReadCapacityAutoScalingSettingsUpdate != null) {
            serializer.writeStruct($SCHEMA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS_UPDATE, provisionedReadCapacityAutoScalingSettingsUpdate);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, indexName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_READ_CAPACITY_UNITS, member, provisionedReadCapacityUnits);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS_UPDATE, member, provisionedReadCapacityAutoScalingSettingsUpdate);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ReplicaGlobalSecondaryIndexSettingsUpdate}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.indexName(this.indexName);
        builder.provisionedReadCapacityUnits(this.provisionedReadCapacityUnits);
        builder.provisionedReadCapacityAutoScalingSettingsUpdate(this.provisionedReadCapacityAutoScalingSettingsUpdate);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ReplicaGlobalSecondaryIndexSettingsUpdate}.
     */
    public static final class Builder implements ShapeBuilder<ReplicaGlobalSecondaryIndexSettingsUpdate> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String indexName;
        private Long provisionedReadCapacityUnits;
        private AutoScalingSettingsUpdate provisionedReadCapacityAutoScalingSettingsUpdate;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the global secondary index. The name must be unique among all other indexes on this table.
         *
         * <p><strong>Required</strong>
         * @return this builder.
         */
        public Builder indexName(String indexName) {
            this.indexName = Objects.requireNonNull(indexName, "indexName cannot be null");
            tracker.setMember($SCHEMA_INDEX_NAME);
            return this;
        }

        /**
         * The maximum number of strongly consistent reads consumed per second before DynamoDB returns a
         * <code>ThrottlingException</code>.
         *
         * @return this builder.
         */
        public Builder provisionedReadCapacityUnits(Long provisionedReadCapacityUnits) {
            this.provisionedReadCapacityUnits = provisionedReadCapacityUnits;
            return this;
        }

        /**
         * Auto scaling settings for managing a global secondary index replica's read capacity units.
         *
         * @return this builder.
         */
        public Builder provisionedReadCapacityAutoScalingSettingsUpdate(AutoScalingSettingsUpdate provisionedReadCapacityAutoScalingSettingsUpdate) {
            this.provisionedReadCapacityAutoScalingSettingsUpdate = provisionedReadCapacityAutoScalingSettingsUpdate;
            return this;
        }

        @Override
        public ReplicaGlobalSecondaryIndexSettingsUpdate build() {
            tracker.validate();
            return new ReplicaGlobalSecondaryIndexSettingsUpdate(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> indexName((String) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, value));
                case 1 -> provisionedReadCapacityUnits((Long) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_READ_CAPACITY_UNITS, member, value));
                case 2 -> provisionedReadCapacityAutoScalingSettingsUpdate((AutoScalingSettingsUpdate) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS_UPDATE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ReplicaGlobalSecondaryIndexSettingsUpdate> errorCorrection() {
            if (tracker.allSet()) {
                return this;
            }
            if (!tracker.checkMember($SCHEMA_INDEX_NAME)) {
                indexName("");
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
                    case 0 -> builder.indexName(de.readString(member));
                    case 1 -> builder.provisionedReadCapacityUnits(de.readLong(member));
                    case 2 -> builder.provisionedReadCapacityAutoScalingSettingsUpdate(AutoScalingSettingsUpdate.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
