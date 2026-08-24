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

@SmithyGenerated
public final class UpdateTableReplicaAutoScalingOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.UPDATE_TABLE_REPLICA_AUTO_SCALING_OUTPUT;
    private static final Schema $SCHEMA_TABLE_AUTO_SCALING_DESCRIPTION = $SCHEMA.member("TableAutoScalingDescription");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient TableAutoScalingDescription tableAutoScalingDescription;

    private UpdateTableReplicaAutoScalingOutput(Builder builder) {
        this.tableAutoScalingDescription = builder.tableAutoScalingDescription;
    }

    /**
     * Returns information about the auto scaling settings of a table with replicas.
     */
    public TableAutoScalingDescription getTableAutoScalingDescription() {
        return tableAutoScalingDescription;
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
        UpdateTableReplicaAutoScalingOutput that = (UpdateTableReplicaAutoScalingOutput) other;
        return Objects.equals(this.tableAutoScalingDescription, that.tableAutoScalingDescription);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableAutoScalingDescription);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (tableAutoScalingDescription != null) {
            serializer.writeStruct($SCHEMA_TABLE_AUTO_SCALING_DESCRIPTION, tableAutoScalingDescription);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_AUTO_SCALING_DESCRIPTION, member, tableAutoScalingDescription);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link UpdateTableReplicaAutoScalingOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableAutoScalingDescription(this.tableAutoScalingDescription);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link UpdateTableReplicaAutoScalingOutput}.
     */
    public static final class Builder implements ShapeBuilder<UpdateTableReplicaAutoScalingOutput> {
        private TableAutoScalingDescription tableAutoScalingDescription;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Returns information about the auto scaling settings of a table with replicas.
         *
         * @return this builder.
         */
        public Builder tableAutoScalingDescription(TableAutoScalingDescription tableAutoScalingDescription) {
            this.tableAutoScalingDescription = tableAutoScalingDescription;
            return this;
        }

        @Override
        public UpdateTableReplicaAutoScalingOutput build() {
            return new UpdateTableReplicaAutoScalingOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableAutoScalingDescription((TableAutoScalingDescription) SchemaUtils.validateSameMember($SCHEMA_TABLE_AUTO_SCALING_DESCRIPTION, member, value));
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
                    case 0 -> builder.tableAutoScalingDescription(TableAutoScalingDescription.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
