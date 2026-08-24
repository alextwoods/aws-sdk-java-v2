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
 * Represents the auto scaling configuration for a global table.
 */
@SmithyGenerated
public final class TableAutoScalingDescription implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.TABLE_AUTO_SCALING_DESCRIPTION;
    private static final Schema $SCHEMA_TABLE_NAME = $SCHEMA.member("TableName");
    private static final Schema $SCHEMA_TABLE_STATUS = $SCHEMA.member("TableStatus");
    private static final Schema $SCHEMA_REPLICAS = $SCHEMA.member("Replicas");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient String tableName;
    private final transient TableStatus tableStatus;
    private final transient List<ReplicaAutoScalingDescription> replicas;

    private TableAutoScalingDescription(Builder builder) {
        this.tableName = builder.tableName;
        this.tableStatus = builder.tableStatus;
        this.replicas = builder.replicas == null ? null : Collections.unmodifiableList(builder.replicas);
    }

    /**
     * The name of the table.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * The current state of the table:
     *
     * <ul>
     *   <li>
     *     <code>CREATING</code> - The table is being created.
     *   </li>
     *   <li>
     *     <code>UPDATING</code> - The table is being updated.
     *   </li>
     *   <li>
     *     <code>DELETING</code> - The table is being deleted.
     *   </li>
     *   <li>
     *     <code>ACTIVE</code> - The table is ready for use.
     *   </li>
     * </ul>
     */
    public TableStatus getTableStatus() {
        return tableStatus;
    }

    /**
     * Represents replicas of the global table.
     */
    public List<ReplicaAutoScalingDescription> getReplicas() {
        if (replicas == null) {
            return Collections.emptyList();
        }
        return replicas;
    }

    public boolean hasReplicas() {
        return replicas != null;
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
        TableAutoScalingDescription that = (TableAutoScalingDescription) other;
        return Objects.equals(this.tableName, that.tableName)
               && Objects.equals(this.tableStatus, that.tableStatus)
               && Objects.equals(this.replicas, that.replicas);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(tableName);
        $hc = 31 * $hc + Objects.hashCode(tableStatus);
        $hc = 31 * $hc + Objects.hashCode(replicas);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (tableName != null) {
            serializer.writeString($SCHEMA_TABLE_NAME, tableName);
        }
        if (tableStatus != null) {
            serializer.writeString($SCHEMA_TABLE_STATUS, tableStatus.getValue());
        }
        if (replicas != null) {
            serializer.writeList($SCHEMA_REPLICAS, replicas, replicas.size(), SharedSerde.ReplicaAutoScalingDescriptionListSerializer.INSTANCE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, tableName);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_TABLE_STATUS, member, tableStatus);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_REPLICAS, member, replicas);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link TableAutoScalingDescription}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.tableName(this.tableName);
        builder.tableStatus(this.tableStatus);
        builder.replicas(this.replicas);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TableAutoScalingDescription}.
     */
    public static final class Builder implements ShapeBuilder<TableAutoScalingDescription> {
        private String tableName;
        private TableStatus tableStatus;
        private List<ReplicaAutoScalingDescription> replicas;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The name of the table.
         *
         * @return this builder.
         */
        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        /**
         * The current state of the table:
         *
         * <ul>
         *   <li>
         *     <code>CREATING</code> - The table is being created.
         *   </li>
         *   <li>
         *     <code>UPDATING</code> - The table is being updated.
         *   </li>
         *   <li>
         *     <code>DELETING</code> - The table is being deleted.
         *   </li>
         *   <li>
         *     <code>ACTIVE</code> - The table is ready for use.
         *   </li>
         * </ul>
         *
         * @return this builder.
         */
        public Builder tableStatus(TableStatus tableStatus) {
            this.tableStatus = tableStatus;
            return this;
        }

        /**
         * Represents replicas of the global table.
         *
         * @return this builder.
         */
        public Builder replicas(List<ReplicaAutoScalingDescription> replicas) {
            this.replicas = replicas;
            return this;
        }

        @Override
        public TableAutoScalingDescription build() {
            return new TableAutoScalingDescription(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> tableName((String) SchemaUtils.validateSameMember($SCHEMA_TABLE_NAME, member, value));
                case 1 -> tableStatus((TableStatus) SchemaUtils.validateSameMember($SCHEMA_TABLE_STATUS, member, value));
                case 2 -> replicas((List<ReplicaAutoScalingDescription>) SchemaUtils.validateSameMember($SCHEMA_REPLICAS, member, value));
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
                    case 0 -> builder.tableName(de.readString(member));
                    case 1 -> builder.tableStatus(TableStatus.builder().deserializeMember(de, member).build());
                    case 2 -> builder.replicas(SharedSerde.deserializeReplicaAutoScalingDescriptionList(member, de));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
