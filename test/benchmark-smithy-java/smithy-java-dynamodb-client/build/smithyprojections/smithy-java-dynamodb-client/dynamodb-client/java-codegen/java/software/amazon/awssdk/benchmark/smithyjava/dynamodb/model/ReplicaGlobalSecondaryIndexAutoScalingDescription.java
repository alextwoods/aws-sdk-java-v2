package software.amazon.awssdk.benchmark.smithyjava.dynamodb.model;

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
 * Represents the auto scaling configuration for a replica global secondary index.
 */
@SmithyGenerated
public final class ReplicaGlobalSecondaryIndexAutoScalingDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.REPLICA_GLOBAL_SECONDARY_INDEX_AUTO_SCALING_DESCRIPTION;
    private static final Schema $SCHEMA_INDEX_NAME = $SCHEMA.member("IndexName");
    private static final Schema $SCHEMA_INDEX_STATUS = $SCHEMA.member("IndexStatus");
    private static final Schema $SCHEMA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS = $SCHEMA.member("ProvisionedReadCapacityAutoScalingSettings");
    private static final Schema $SCHEMA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_SETTINGS = $SCHEMA.member("ProvisionedWriteCapacityAutoScalingSettings");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String indexName;
    private final transient IndexStatus indexStatus;
    private final transient AutoScalingSettingsDescription provisionedReadCapacityAutoScalingSettings;
    private final transient AutoScalingSettingsDescription provisionedWriteCapacityAutoScalingSettings;

    private ReplicaGlobalSecondaryIndexAutoScalingDescription(Builder builder) {
        this.indexName = builder.indexName;
        this.indexStatus = builder.indexStatus;
        this.provisionedReadCapacityAutoScalingSettings = builder.provisionedReadCapacityAutoScalingSettings;
        this.provisionedWriteCapacityAutoScalingSettings = builder.provisionedWriteCapacityAutoScalingSettings;
    }

    /**
     * The name of the global secondary index.
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * The current state of the replica global secondary index:
     *
     * <ul>
     *   <li>
     *     <code>CREATING</code> - The index is being created.
     *   </li>
     *   <li>
     *     <code>UPDATING</code> - The table/index configuration is being updated. The table/index remains available
     *     for data operations when <code>UPDATING</code>
     *   </li>
     *   <li>
     *     <code>DELETING</code> - The index is being deleted.
     *   </li>
     *   <li>
     *     <code>ACTIVE</code> - The index is ready for use.
     *   </li>
     * </ul>
     */
    public IndexStatus getIndexStatus() {
        return indexStatus;
    }

    public AutoScalingSettingsDescription getProvisionedReadCapacityAutoScalingSettings() {
        return provisionedReadCapacityAutoScalingSettings;
    }

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
        ReplicaGlobalSecondaryIndexAutoScalingDescription that = (ReplicaGlobalSecondaryIndexAutoScalingDescription) other;
        return Objects.equals(this.indexName, that.indexName)
               && Objects.equals(this.indexStatus, that.indexStatus)
               && Objects.equals(this.provisionedReadCapacityAutoScalingSettings, that.provisionedReadCapacityAutoScalingSettings)
               && Objects.equals(this.provisionedWriteCapacityAutoScalingSettings, that.provisionedWriteCapacityAutoScalingSettings);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(indexName);
        $hc = 31 * $hc + Objects.hashCode(indexStatus);
        $hc = 31 * $hc + Objects.hashCode(provisionedReadCapacityAutoScalingSettings);
        $hc = 31 * $hc + Objects.hashCode(provisionedWriteCapacityAutoScalingSettings);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (indexName != null) {
            serializer.writeString($SCHEMA_INDEX_NAME, indexName);
        }
        if (indexStatus != null) {
            serializer.writeString($SCHEMA_INDEX_STATUS, indexStatus.getValue());
        }
        if (provisionedReadCapacityAutoScalingSettings != null) {
            serializer.writeStruct($SCHEMA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS, provisionedReadCapacityAutoScalingSettings);
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
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS, member, provisionedReadCapacityAutoScalingSettings);
            case 3 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_SETTINGS, member, provisionedWriteCapacityAutoScalingSettings);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ReplicaGlobalSecondaryIndexAutoScalingDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.indexName(this.indexName);
        builder.indexStatus(this.indexStatus);
        builder.provisionedReadCapacityAutoScalingSettings(this.provisionedReadCapacityAutoScalingSettings);
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
     * Builder for {@link ReplicaGlobalSecondaryIndexAutoScalingDescription}.
     */
    public static final class Builder implements ShapeBuilder<ReplicaGlobalSecondaryIndexAutoScalingDescription> {
        private String indexName;
        private IndexStatus indexStatus;
        private AutoScalingSettingsDescription provisionedReadCapacityAutoScalingSettings;
        private AutoScalingSettingsDescription provisionedWriteCapacityAutoScalingSettings;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the global secondary index.
         *
         * @return this builder.
         */
        public Builder indexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        /**
         * The current state of the replica global secondary index:
         *
         * <ul>
         *   <li>
         *     <code>CREATING</code> - The index is being created.
         *   </li>
         *   <li>
         *     <code>UPDATING</code> - The table/index configuration is being updated. The table/index remains available
         *     for data operations when <code>UPDATING</code>
         *   </li>
         *   <li>
         *     <code>DELETING</code> - The index is being deleted.
         *   </li>
         *   <li>
         *     <code>ACTIVE</code> - The index is ready for use.
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
         * @return this builder.
         */
        public Builder provisionedReadCapacityAutoScalingSettings(AutoScalingSettingsDescription provisionedReadCapacityAutoScalingSettings) {
            this.provisionedReadCapacityAutoScalingSettings = provisionedReadCapacityAutoScalingSettings;
            return this;
        }

        /**
         * @return this builder.
         */
        public Builder provisionedWriteCapacityAutoScalingSettings(AutoScalingSettingsDescription provisionedWriteCapacityAutoScalingSettings) {
            this.provisionedWriteCapacityAutoScalingSettings = provisionedWriteCapacityAutoScalingSettings;
            return this;
        }

        @Override
        public ReplicaGlobalSecondaryIndexAutoScalingDescription build() {
            return new ReplicaGlobalSecondaryIndexAutoScalingDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> indexName((String) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, value));
                case 1 -> indexStatus((IndexStatus) SchemaUtils.validateSameMember($SCHEMA_INDEX_STATUS, member, value));
                case 2 -> provisionedReadCapacityAutoScalingSettings((AutoScalingSettingsDescription) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_READ_CAPACITY_AUTO_SCALING_SETTINGS, member, value));
                case 3 -> provisionedWriteCapacityAutoScalingSettings((AutoScalingSettingsDescription) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_SETTINGS, member, value));
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
                    case 0 -> builder.indexName(de.readString(member));
                    case 1 -> builder.indexStatus(IndexStatus.builder().deserializeMember(de, member).build());
                    case 2 -> builder.provisionedReadCapacityAutoScalingSettings(AutoScalingSettingsDescription.builder().deserializeMember(de, member).build());
                    case 3 -> builder.provisionedWriteCapacityAutoScalingSettings(AutoScalingSettingsDescription.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
