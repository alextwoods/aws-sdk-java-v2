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
 *     A new replica to be added to an existing global table.
 *   </li>
 *   <li>
 *     New parameters for an existing replica.
 *   </li>
 *   <li>
 *     An existing replica to be removed from an existing global table.
 *   </li>
 * </ul>
 */
@SmithyGenerated
public final class ReplicaUpdate implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.REPLICA_UPDATE;
    private static final Schema $SCHEMA_CREATE = $SCHEMA.member("Create");
    private static final Schema $SCHEMA_DELETE = $SCHEMA.member("Delete");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient CreateReplicaAction create;
    private final transient DeleteReplicaAction delete;

    private ReplicaUpdate(Builder builder) {
        this.create = builder.create;
        this.delete = builder.delete;
    }

    /**
     * The parameters required for creating a replica on an existing global table.
     */
    public CreateReplicaAction getCreate() {
        return create;
    }

    /**
     * The name of the existing replica to be removed.
     */
    public DeleteReplicaAction getDelete() {
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
        ReplicaUpdate that = (ReplicaUpdate) other;
        return Objects.equals(this.create, that.create)
               && Objects.equals(this.delete, that.delete);
    }

    @Override
    public int hashCode() {
        int $hc = Objects.hashCode(create);
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
        if (delete != null) {
            serializer.writeStruct($SCHEMA_DELETE, delete);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_CREATE, member, create);
            case 1 -> (T) SchemaUtils.validateSameMember($SCHEMA_DELETE, member, delete);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link ReplicaUpdate}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.create(this.create);
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
     * Builder for {@link ReplicaUpdate}.
     */
    public static final class Builder implements ShapeBuilder<ReplicaUpdate> {
        private CreateReplicaAction create;
        private DeleteReplicaAction delete;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The parameters required for creating a replica on an existing global table.
         *
         * @return this builder.
         */
        public Builder create(CreateReplicaAction create) {
            this.create = create;
            return this;
        }

        /**
         * The name of the existing replica to be removed.
         *
         * @return this builder.
         */
        public Builder delete(DeleteReplicaAction delete) {
            this.delete = delete;
            return this;
        }

        @Override
        public ReplicaUpdate build() {
            return new ReplicaUpdate(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> create((CreateReplicaAction) SchemaUtils.validateSameMember($SCHEMA_CREATE, member, value));
                case 1 -> delete((DeleteReplicaAction) SchemaUtils.validateSameMember($SCHEMA_DELETE, member, value));
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
                    case 0 -> builder.create(CreateReplicaAction.builder().deserializeMember(de, member).build());
                    case 1 -> builder.delete(DeleteReplicaAction.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
