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
 * Represents one of the following:
 *
 * <ul>
 *   <li>
 *     A new replica to be added to an existing regional table or global table. This request invokes the <code>
 *     CreateTableReplica</code> action in the destination Region.
 *   </li>
 *   <li>
 *     New parameters for an existing replica. This request invokes the <code>UpdateTable</code> action in the
 *     destination Region.
 *   </li>
 *   <li>
 *     An existing replica to be deleted. The request invokes the <code>DeleteTableReplica</code> action in the
 *     destination Region, deleting the replica and all if its items in the destination Region.
 *   </li>
 * </ul>
 *
 * <p>When you manually remove a table or global table replica, you do not automatically remove any associated scalable
 * targets, scaling policies, or CloudWatch alarms.
 */
@SmithyGenerated
public final class ReplicationGroupUpdate implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.REPLICATION_GROUP_UPDATE;
    private static final Schema $SCHEMA_CREATE = $SCHEMA.member("Create");
    private static final Schema $SCHEMA_UPDATE = $SCHEMA.member("Update");
    private static final Schema $SCHEMA_DELETE = $SCHEMA.member("Delete");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient CreateReplicationGroupMemberAction create;
    private final transient UpdateReplicationGroupMemberAction update;
    private final transient DeleteReplicationGroupMemberAction delete;

    private ReplicationGroupUpdate(Builder builder) {
        this.create = builder.create;
        this.update = builder.update;
        this.delete = builder.delete;
    }

    /**
     * The parameters required for creating a replica for the table.
     */
    public CreateReplicationGroupMemberAction getCreate() {
        return create;
    }

    /**
     * The parameters required for updating a replica for the table.
     */
    public UpdateReplicationGroupMemberAction getUpdate() {
        return update;
    }

    /**
     * The parameters required for deleting a replica for the table.
     */
    public DeleteReplicationGroupMemberAction getDelete() {
        return delete;
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
        ReplicationGroupUpdate that = (ReplicationGroupUpdate) other;
        return Objects.equals(this.create, that.create)
               && Objects.equals(this.update, that.update)
               && Objects.equals(this.delete, that.delete);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(create);
        $hc = 31 * $hc + Objects.hashCode(update);
        $hc = 31 * $hc + Objects.hashCode(delete);
        return $hc;
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (create != null) {
            serializer.writeStruct($SCHEMA_CREATE, create);
        }
        if (update != null) {
            serializer.writeStruct($SCHEMA_UPDATE, update);
        }
        if (delete != null) {
            serializer.writeStruct($SCHEMA_DELETE, delete);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_CREATE, member, create);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_UPDATE, member, update);
            case 2 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELETE, member, delete);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ReplicationGroupUpdate}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.create(this.create);
        builder.update(this.update);
        builder.delete(this.delete);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ReplicationGroupUpdate}.
     */
    public static final class Builder implements ShapeBuilder<ReplicationGroupUpdate> {
        private CreateReplicationGroupMemberAction create;
        private UpdateReplicationGroupMemberAction update;
        private DeleteReplicationGroupMemberAction delete;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The parameters required for creating a replica for the table.
         *
         * @return this builder.
         */
        public Builder create(CreateReplicationGroupMemberAction create) {
            this.create = create;
            return this;
        }

        /**
         * The parameters required for updating a replica for the table.
         *
         * @return this builder.
         */
        public Builder update(UpdateReplicationGroupMemberAction update) {
            this.update = update;
            return this;
        }

        /**
         * The parameters required for deleting a replica for the table.
         *
         * @return this builder.
         */
        public Builder delete(DeleteReplicationGroupMemberAction delete) {
            this.delete = delete;
            return this;
        }

        @Override
        public ReplicationGroupUpdate build() {
            return new ReplicationGroupUpdate(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> create((CreateReplicationGroupMemberAction) SchemaUtils.validateSameMember($SCHEMA_CREATE, member, value));
                case 1 -> update((UpdateReplicationGroupMemberAction) SchemaUtils.validateSameMember($SCHEMA_UPDATE, member, value));
                case 2 -> delete((DeleteReplicationGroupMemberAction) SchemaUtils.validateSameMember($SCHEMA_DELETE, member, value));
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
                    case 0 -> builder.create(CreateReplicationGroupMemberAction.builder().deserializeMember(de, member).build());
                    case 1 -> builder.update(UpdateReplicationGroupMemberAction.builder().deserializeMember(de, member).build());
                    case 2 -> builder.delete(DeleteReplicationGroupMemberAction.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
