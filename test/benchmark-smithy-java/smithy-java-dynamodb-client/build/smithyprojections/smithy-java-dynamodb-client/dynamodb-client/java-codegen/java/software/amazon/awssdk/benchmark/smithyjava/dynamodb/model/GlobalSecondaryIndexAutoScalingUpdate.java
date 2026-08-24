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
 * Represents the auto scaling settings of a global secondary index for a global table that will be modified.
 */
@SmithyGenerated
public final class GlobalSecondaryIndexAutoScalingUpdate implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GLOBAL_SECONDARY_INDEX_AUTO_SCALING_UPDATE;
    private static final Schema $SCHEMA_INDEX_NAME = $SCHEMA.member("IndexName");
    private static final Schema $SCHEMA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_UPDATE = $SCHEMA.member("ProvisionedWriteCapacityAutoScalingUpdate");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String indexName;
    private final transient AutoScalingSettingsUpdate provisionedWriteCapacityAutoScalingUpdate;

    private GlobalSecondaryIndexAutoScalingUpdate(Builder builder) {
        this.indexName = builder.indexName;
        this.provisionedWriteCapacityAutoScalingUpdate = builder.provisionedWriteCapacityAutoScalingUpdate;
    }

    /**
     * The name of the global secondary index.
     */
    public String getIndexName() {
        return indexName;
    }

    public AutoScalingSettingsUpdate getProvisionedWriteCapacityAutoScalingUpdate() {
        return provisionedWriteCapacityAutoScalingUpdate;
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
        GlobalSecondaryIndexAutoScalingUpdate that = (GlobalSecondaryIndexAutoScalingUpdate) other;
        return Objects.equals(this.indexName, that.indexName)
               && Objects.equals(this.provisionedWriteCapacityAutoScalingUpdate, that.provisionedWriteCapacityAutoScalingUpdate);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(indexName);
        $hc = 31 * $hc + Objects.hashCode(provisionedWriteCapacityAutoScalingUpdate);
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
        if (provisionedWriteCapacityAutoScalingUpdate != null) {
            serializer.writeStruct($SCHEMA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_UPDATE, provisionedWriteCapacityAutoScalingUpdate);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, indexName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_UPDATE, member, provisionedWriteCapacityAutoScalingUpdate);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link GlobalSecondaryIndexAutoScalingUpdate}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.indexName(this.indexName);
        builder.provisionedWriteCapacityAutoScalingUpdate(this.provisionedWriteCapacityAutoScalingUpdate);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GlobalSecondaryIndexAutoScalingUpdate}.
     */
    public static final class Builder implements ShapeBuilder<GlobalSecondaryIndexAutoScalingUpdate> {
        private String indexName;
        private AutoScalingSettingsUpdate provisionedWriteCapacityAutoScalingUpdate;

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
         * @return this builder.
         */
        public Builder provisionedWriteCapacityAutoScalingUpdate(AutoScalingSettingsUpdate provisionedWriteCapacityAutoScalingUpdate) {
            this.provisionedWriteCapacityAutoScalingUpdate = provisionedWriteCapacityAutoScalingUpdate;
            return this;
        }

        @Override
        public GlobalSecondaryIndexAutoScalingUpdate build() {
            return new GlobalSecondaryIndexAutoScalingUpdate(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> indexName((String) SchemaUtils.validateSameMember($SCHEMA_INDEX_NAME, member, value));
                case 1 -> provisionedWriteCapacityAutoScalingUpdate((AutoScalingSettingsUpdate) SchemaUtils.validateSameMember($SCHEMA_PROVISIONED_WRITE_CAPACITY_AUTO_SCALING_UPDATE, member, value));
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
                    case 1 -> builder.provisionedWriteCapacityAutoScalingUpdate(AutoScalingSettingsUpdate.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
