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
public final class GlobalTableGlobalSecondaryIndexSettingsUpdate implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GLOBAL_TABLE_GLOBAL_SECONDARY_INDEX_SETTINGS_UPDATE;
    private static final Schema $SCHEMA_INDEX_NAME = $SCHEMA.member("IndexName");
    private static final Schema $SCHEMA_PROVISIONED_WRITE_CAPACITY_UNITS = $SCHEMA.member("ProvisionedWriteCapacityUnits");
    private static final Schema $SCHEMA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_SETTINGS_UPDATE = $SCHEMA.member("ProvisionedWriteCapacityAutoScalingSettingsUpdate");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String indexName;
    private final transient Long provisionedWriteCapacityUnits;
    private final transient AutoScalingSettingsUpdate provisionedWriteCapacityAutoScalingSettingsUpdate;

    private GlobalTableGlobalSecondaryIndexSettingsUpdate(Builder builder) {
        this.indexName = builder.indexName;
        this.provisionedWriteCapacityUnits = builder.provisionedWriteCapacityUnits;
        this.provisionedWriteCapacityAutoScalingSettingsUpdate = builder.provisionedWriteCapacityAutoScalingSettingsUpdate;
    }

    /**
     * The name of the global secondary index. The name must be unique among all other indexes on this table.
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * The maximum number of writes consumed per second before DynamoDB returns a <code>ThrottlingException.</code>
     */
    public Long getProvisionedWriteCapacityUnits() {
        return provisionedWriteCapacityUnits;
    }

    /**
     * Auto scaling settings for managing a global secondary index's write capacity units.
     */
    public AutoScalingSettingsUpdate getProvisionedWriteCapacityAutoScalingSettingsUpdate() {
        return provisionedWriteCapacityAutoScalingSettingsUpdate;
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
        GlobalTableGlobalSecondaryIndexSettingsUpdate that = (GlobalTableGlobalSecondaryIndexSettingsUpdate) other;
        return Objects.equals(this.provisionedWriteCapacityUnits, that.provisionedWriteCapacityUnits)
               && Objects.equals(this.indexName, that.indexName)
               && Objects.equals(this.provisionedWriteCapacityAutoScalingSettingsUpdate, that.provisionedWriteCapacityAutoScalingSettingsUpdate);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(indexName);
        $hc = 31 * $hc + Objects.hashCode(provisionedWriteCapacityUnits);
        $hc = 31 * $hc + Objects.hashCode(provisionedWriteCapacityAutoScalingSettingsUpdate);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        serializer.writeString($SCHEMA_INDEX_NAME, indexName);
        if (provisionedWriteCapacityUnits != null) {
            serializer.writeLong($SCHEMA_PROVISIONED_WRITE_CAPACITY_UNITS, provisionedWriteCapacityUnits);
        }
        if (provisionedWriteCapacityAutoScalingSettingsUpdate != null) {
            serializer.writeStruct($SCHEMA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_SETTINGS_UPDATE, provisionedWriteCapacityAutoScalingSettingsUpdate);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, indexName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_WRITE_CAPACITY_UNITS, member, provisionedWriteCapacityUnits);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_SETTINGS_UPDATE, member, provisionedWriteCapacityAutoScalingSettingsUpdate);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GlobalTableGlobalSecondaryIndexSettingsUpdate}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.indexName(this.indexName);
        builder.provisionedWriteCapacityUnits(this.provisionedWriteCapacityUnits);
        builder.provisionedWriteCapacityAutoScalingSettingsUpdate(this.provisionedWriteCapacityAutoScalingSettingsUpdate);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GlobalTableGlobalSecondaryIndexSettingsUpdate}.
     */
    public static final class Builder implements ShapeBuilder<GlobalTableGlobalSecondaryIndexSettingsUpdate> {
        private final PresenceTracker tracker = PresenceTracker.of($SCHEMA);
        private String indexName;
        private Long provisionedWriteCapacityUnits;
        private AutoScalingSettingsUpdate provisionedWriteCapacityAutoScalingSettingsUpdate;

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
         * The maximum number of writes consumed per second before DynamoDB returns a <code>ThrottlingException.</code>
         *
         * @return this builder.
         */
        public Builder provisionedWriteCapacityUnits(Long provisionedWriteCapacityUnits) {
            this.provisionedWriteCapacityUnits = provisionedWriteCapacityUnits;
            return this;
        }

        /**
         * Auto scaling settings for managing a global secondary index's write capacity units.
         *
         * @return this builder.
         */
        public Builder provisionedWriteCapacityAutoScalingSettingsUpdate(AutoScalingSettingsUpdate provisionedWriteCapacityAutoScalingSettingsUpdate) {
            this.provisionedWriteCapacityAutoScalingSettingsUpdate = provisionedWriteCapacityAutoScalingSettingsUpdate;
            return this;
        }

        @Override
        public GlobalTableGlobalSecondaryIndexSettingsUpdate build() {
            tracker.validate();
            return new GlobalTableGlobalSecondaryIndexSettingsUpdate(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> indexName((String) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, value));
                case 1 -> provisionedWriteCapacityUnits((Long) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_WRITE_CAPACITY_UNITS, member, value));
                case 2 -> provisionedWriteCapacityAutoScalingSettingsUpdate((AutoScalingSettingsUpdate) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_SETTINGS_UPDATE, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public ShapeBuilder<GlobalTableGlobalSecondaryIndexSettingsUpdate> errorCorrection() {
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
                    case 1 -> builder.provisionedWriteCapacityUnits(de.readLong(member));
                    case 2 -> builder.provisionedWriteCapacityAutoScalingSettingsUpdate(AutoScalingSettingsUpdate.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
