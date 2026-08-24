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
 * Represents the properties of a global secondary index.
 */
@SmithyGenerated
public final class ReplicaGlobalSecondaryIndexSettingsDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_SETTINGS_DESCRIPTION;
    private static final Schema $SCHEMA_INDEX_NAME = $SCHEMA.member("IndexName");
    private static final Schema $SCHEMA_INDEX_STATUS = $SCHEMA.member("IndexStatus");
    private static final Schema $SCHEMA_PROVISIONED_READ_CAPACITY_UNITS = $SCHEMA.member("ProvisionedReadCapacityUnits");
    private static final Schema $SCHEMA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS = $SCHEMA.member("ProvisionedReadCapacityAutoScalingSettings");
    private static final Schema $SCHEMA_PROVISIONED_WRITE_CAPACITY_UNITS = $SCHEMA.member("ProvisionedWriteCapacityUnits");
    private static final Schema $SCHEMA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_SETTINGS = $SCHEMA.member("ProvisionedWriteCapacityAutoScalingSettings");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String indexName;
    private final transient IndexStatus indexStatus;
    private final transient Long provisionedReadCapacityUnits;
    private final transient AutoScalingSettingsDescription provisionedReadCapacityAutoScalingSettings;
    private final transient Long provisionedWriteCapacityUnits;
    private final transient AutoScalingSettingsDescription provisionedWriteCapacityAutoScalingSettings;

    private ReplicaGlobalSecondaryIndexSettingsDescription(Builder builder) {
        this.indexName = builder.indexName;
        this.indexStatus = builder.indexStatus;
        this.provisionedReadCapacityUnits = builder.provisionedReadCapacityUnits;
        this.provisionedReadCapacityAutoScalingSettings = builder.provisionedReadCapacityAutoScalingSettings;
        this.provisionedWriteCapacityUnits = builder.provisionedWriteCapacityUnits;
        this.provisionedWriteCapacityAutoScalingSettings = builder.provisionedWriteCapacityAutoScalingSettings;
    }

    /**
     * The name of the global secondary index. The name must be unique among all other indexes on this table.
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * The current status of the global secondary index:
     *
     * <ul>
     *   <li>
     *     <code>CREATING</code> - The global secondary index is being created.
     *   </li>
     *   <li>
     *     <code>UPDATING</code> - The global secondary index is being updated.
     *   </li>
     *   <li>
     *     <code>DELETING</code> - The global secondary index is being deleted.
     *   </li>
     *   <li>
     *     <code>ACTIVE</code> - The global secondary index is ready for use.
     *   </li>
     * </ul>
     */
    public IndexStatus getIndexStatus() {
        return indexStatus;
    }

    /**
     * The maximum number of strongly consistent reads consumed per second before DynamoDB returns a
     * <code>ThrottlingException</code>.
     */
    public Long getProvisionedReadCapacityUnits() {
        return provisionedReadCapacityUnits;
    }

    /**
     * Auto scaling settings for a global secondary index replica's read capacity units.
     */
    public AutoScalingSettingsDescription getProvisionedReadCapacityAutoScalingSettings() {
        return provisionedReadCapacityAutoScalingSettings;
    }

    /**
     * The maximum number of writes consumed per second before DynamoDB returns a <code>ThrottlingException</code>.
     */
    public Long getProvisionedWriteCapacityUnits() {
        return provisionedWriteCapacityUnits;
    }

    /**
     * Auto scaling settings for a global secondary index replica's write capacity units.
     */
    public AutoScalingSettingsDescription getProvisionedWriteCapacityAutoScalingSettings() {
        return provisionedWriteCapacityAutoScalingSettings;
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
        ReplicaGlobalSecondaryIndexSettingsDescription that = (ReplicaGlobalSecondaryIndexSettingsDescription) other;
        return Objects.equals(this.provisionedReadCapacityUnits, that.provisionedReadCapacityUnits)
               && Objects.equals(this.provisionedWriteCapacityUnits, that.provisionedWriteCapacityUnits)
               && Objects.equals(this.indexName, that.indexName)
               && Objects.equals(this.indexStatus, that.indexStatus)
               && Objects.equals(this.provisionedReadCapacityAutoScalingSettings, that.provisionedReadCapacityAutoScalingSettings)
               && Objects.equals(this.provisionedWriteCapacityAutoScalingSettings, that.provisionedWriteCapacityAutoScalingSettings);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(indexName);
        $hc = 31 * $hc + Objects.hashCode(indexStatus);
        $hc = 31 * $hc + Objects.hashCode(provisionedReadCapacityUnits);
        $hc = 31 * $hc + Objects.hashCode(provisionedReadCapacityAutoScalingSettings);
        $hc = 31 * $hc + Objects.hashCode(provisionedWriteCapacityUnits);
        $hc = 31 * $hc + Objects.hashCode(provisionedWriteCapacityAutoScalingSettings);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_INDEX_NAME, indexName);
        if (indexStatus != null) {
            serializer.writeString($SCHEMA_INDEX_STATUS, indexStatus.getValue());
        }
        if (provisionedReadCapacityUnits != null) {
            serializer.writeLong($SCHEMA_PROVISIONED_READ_CAPACITY_UNITS, provisionedReadCapacityUnits);
        }
        if (provisionedReadCapacityAutoScalingSettings != null) {
            serializer.writeStruct($SCHEMA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS, provisionedReadCapacityAutoScalingSettings);
        }
        if (provisionedWriteCapacityUnits != null) {
            serializer.writeLong($SCHEMA_PROVISIONED_WRITE_CAPACITY_UNITS, provisionedWriteCapacityUnits);
        }
        if (provisionedWriteCapacityAutoScalingSettings != null) {
            serializer.writeStruct($SCHEMA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_SETTINGS, provisionedWriteCapacityAutoScalingSettings);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, indexName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_STATUS, member, indexStatus);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_READ_CAPACITY_UNITS, member, provisionedReadCapacityUnits);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS, member, provisionedReadCapacityAutoScalingSettings);
            case 4 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_WRITE_CAPACITY_UNITS, member, provisionedWriteCapacityUnits);
            case 5 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_SETTINGS, member, provisionedWriteCapacityAutoScalingSettings);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ReplicaGlobalSecondaryIndexSettingsDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.indexName(this.indexName);
        builder.indexStatus(this.indexStatus);
        builder.provisionedReadCapacityUnits(this.provisionedReadCapacityUnits);
        builder.provisionedReadCapacityAutoScalingSettings(this.provisionedReadCapacityAutoScalingSettings);
        builder.provisionedWriteCapacityUnits(this.provisionedWriteCapacityUnits);
        builder.provisionedWriteCapacityAutoScalingSettings(this.provisionedWriteCapacityAutoScalingSettings);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ReplicaGlobalSecondaryIndexSettingsDescription}.
     */
    public static final class Builder implements ShapeBuilder<ReplicaGlobalSecondaryIndexSettingsDescription> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String indexName;
        private IndexStatus indexStatus;
        private Long provisionedReadCapacityUnits;
        private AutoScalingSettingsDescription provisionedReadCapacityAutoScalingSettings;
        private Long provisionedWriteCapacityUnits;
        private AutoScalingSettingsDescription provisionedWriteCapacityAutoScalingSettings;

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
         * The current status of the global secondary index:
         *
         * <ul>
         *   <li>
         *     <code>CREATING</code> - The global secondary index is being created.
         *   </li>
         *   <li>
         *     <code>UPDATING</code> - The global secondary index is being updated.
         *   </li>
         *   <li>
         *     <code>DELETING</code> - The global secondary index is being deleted.
         *   </li>
         *   <li>
         *     <code>ACTIVE</code> - The global secondary index is ready for use.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder indexStatus(IndexStatus indexStatus) {
            this.indexStatus = indexStatus;
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
         * Auto scaling settings for a global secondary index replica's read capacity units.
         *
         * @return this builder.
         */
        public Builder provisionedReadCapacityAutoScalingSettings(AutoScalingSettingsDescription provisionedReadCapacityAutoScalingSettings) {
            this.provisionedReadCapacityAutoScalingSettings = provisionedReadCapacityAutoScalingSettings;
            return this;
        }

        /**
         * The maximum number of writes consumed per second before DynamoDB returns a <code>ThrottlingException</code>.
         *
         * @return this builder.
         */
        public Builder provisionedWriteCapacityUnits(Long provisionedWriteCapacityUnits) {
            this.provisionedWriteCapacityUnits = provisionedWriteCapacityUnits;
            return this;
        }

        /**
         * Auto scaling settings for a global secondary index replica's write capacity units.
         *
         * @return this builder.
         */
        public Builder provisionedWriteCapacityAutoScalingSettings(AutoScalingSettingsDescription provisionedWriteCapacityAutoScalingSettings) {
            this.provisionedWriteCapacityAutoScalingSettings = provisionedWriteCapacityAutoScalingSettings;
            return this;
        }

        @Override
        public ReplicaGlobalSecondaryIndexSettingsDescription build() {
            tracker.validate();
            return new ReplicaGlobalSecondaryIndexSettingsDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> indexName((String) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, value));
                case 1 -> indexStatus((IndexStatus) SchemaUtils.validateSameMember($SCHEMA_INDEX_STATUS, member, value));
                case 2 -> provisionedReadCapacityUnits((Long) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_READ_CAPACITY_UNITS, member, value));
                case 3 -> provisionedReadCapacityAutoScalingSettings((AutoScalingSettingsDescription) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS, member, value));
                case 4 -> provisionedWriteCapacityUnits((Long) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_WRITE_CAPACITY_UNITS, member, value));
                case 5 -> provisionedWriteCapacityAutoScalingSettings((AutoScalingSettingsDescription) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_SETTINGS, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<ReplicaGlobalSecondaryIndexSettingsDescription> errorCorrection() {
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
                    case 1 -> builder.indexStatus(IndexStatus.builder().deserializeMember(de, member).build());
                    case 2 -> builder.provisionedReadCapacityUnits(de.readLong(member));
                    case 3 -> builder.provisionedReadCapacityAutoScalingSettings(AutoScalingSettingsDescription.builder().deserializeMember(de, member).build());
                    case 4 -> builder.provisionedWriteCapacityUnits(de.readLong(member));
                    case 5 -> builder.provisionedWriteCapacityAutoScalingSettings(AutoScalingSettingsDescription.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
