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
 * A vector index to be added to or removed from a table.
 */
@SmithyGenerated
public final class VectorIndexUpdate implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.VECTOR_INDEX_UPDATE;
    private static final Schema $SCHEMA_CREATE = $SCHEMA.member("Create");
    private static final Schema $SCHEMA_DELETE = $SCHEMA.member("Delete");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient CreateVectorIndexAction create;
    private final transient DeleteVectorIndexAction delete;

    private VectorIndexUpdate(Builder builder) {
        this.create = builder.create;
        this.delete = builder.delete;
    }

    /**
     * The configuration for creating a new vector index on the table.
     */
    public CreateVectorIndexAction getCreate() {
        return create;
    }

    /**
     * The configuration for deleting an existing vector index from the table.
     */
    public DeleteVectorIndexAction getDelete() {
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
        VectorIndexUpdate that = (VectorIndexUpdate) other;
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
     * @return a builder for {@link VectorIndexUpdate}.
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
     * Builder for {@link VectorIndexUpdate}.
     */
    public static final class Builder implements ShapeBuilder<VectorIndexUpdate> {
        private CreateVectorIndexAction create;
        private DeleteVectorIndexAction delete;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * The configuration for creating a new vector index on the table.
         *
         * @return this builder.
         */
        public Builder create(CreateVectorIndexAction create) {
            this.create = create;
            return this;
        }

        /**
         * The configuration for deleting an existing vector index from the table.
         *
         * @return this builder.
         */
        public Builder delete(DeleteVectorIndexAction delete) {
            this.delete = delete;
            return this;
        }

        @Override
        public VectorIndexUpdate build() {
            return new VectorIndexUpdate(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> create((CreateVectorIndexAction) SchemaUtils.validateSameMember($SCHEMA_CREATE, member, value));
                case 1 -> delete((DeleteVectorIndexAction) SchemaUtils.validateSameMember($SCHEMA_DELETE, member, value));
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
                    case 0 -> builder.create(CreateVectorIndexAction.builder().deserializeMember(de, member).build());
                    case 1 -> builder.delete(DeleteVectorIndexAction.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
