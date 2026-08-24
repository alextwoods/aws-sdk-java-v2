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
 *     A new witness to be added to a new global table.
 *   </li>
 *   <li>
 *     An existing witness to be removed from an existing global table.
 *   </li>
 * </ul>
 *
 * <p>You can configure one witness per MRSC global table.
 */
@SmithyGenerated
public final class GlobalTableWitnessGroupUpdate implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.GLOBAL_TABLE_WITNESS_GROUP_UPDATE;
    private static final Schema $SCHEMA_CREATE = $SCHEMA.member("Create");
    private static final Schema $SCHEMA_DELETE = $SCHEMA.member("Delete");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient CreateGlobalTableWitnessGroupMemberAction create;
    private final transient DeleteGlobalTableWitnessGroupMemberAction delete;

    private GlobalTableWitnessGroupUpdate(Builder builder) {
        this.create = builder.create;
        this.delete = builder.delete;
    }

    /**
     * Specifies a witness Region to be added to a new MRSC global table. The witness must be added when creating the
     * MRSC global table.
     */
    public CreateGlobalTableWitnessGroupMemberAction getCreate() {
        return create;
    }

    /**
     * Specifies a witness Region to be removed from an existing global table. Must be done in conjunction with removing
     * a replica. The deletion of both a witness and replica converts the remaining replica to a single-Region DynamoDB
     * table.
     */
    public DeleteGlobalTableWitnessGroupMemberAction getDelete() {
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
        GlobalTableWitnessGroupUpdate that = (GlobalTableWitnessGroupUpdate) other;
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
     * @return a builder for {@link GlobalTableWitnessGroupUpdate}.
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
     * Builder for {@link GlobalTableWitnessGroupUpdate}.
     */
    public static final class Builder implements ShapeBuilder<GlobalTableWitnessGroupUpdate> {
        private CreateGlobalTableWitnessGroupMemberAction create;
        private DeleteGlobalTableWitnessGroupMemberAction delete;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * Specifies a witness Region to be added to a new MRSC global table. The witness must be added when creating the
         * MRSC global table.
         *
         * @return this builder.
         */
        public Builder create(CreateGlobalTableWitnessGroupMemberAction create) {
            this.create = create;
            return this;
        }

        /**
         * Specifies a witness Region to be removed from an existing global table. Must be done in conjunction with removing
         * a replica. The deletion of both a witness and replica converts the remaining replica to a single-Region DynamoDB
         * table.
         *
         * @return this builder.
         */
        public Builder delete(DeleteGlobalTableWitnessGroupMemberAction delete) {
            this.delete = delete;
            return this;
        }

        @Override
        public GlobalTableWitnessGroupUpdate build() {
            return new GlobalTableWitnessGroupUpdate(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> create((CreateGlobalTableWitnessGroupMemberAction) SchemaUtils.validateSameMember($SCHEMA_CREATE, member, value));
                case 1 -> delete((DeleteGlobalTableWitnessGroupMemberAction) SchemaUtils.validateSameMember($SCHEMA_DELETE, member, value));
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
                    case 0 -> builder.create(CreateGlobalTableWitnessGroupMemberAction.builder().deserializeMember(de, member).build());
                    case 1 -> builder.delete(DeleteGlobalTableWitnessGroupMemberAction.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
